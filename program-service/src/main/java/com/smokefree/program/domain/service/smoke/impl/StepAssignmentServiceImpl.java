package com.smokefree.program.domain.service.smoke.impl;


import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.PlanStepRepo;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.StepAssignmentRepository;

import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StepAssignmentServiceImpl implements StepAssignmentService {

    private final StepAssignmentRepository stepRepo;
    private final ProgramRepository programRepo;
    private final PlanStepRepo planStepRepo;
    @Override
    @Transactional(readOnly = true)
    public List<StepAssignment> listByProgram(UUID programId) {
        return stepRepo.findByProgramIdOrderByStepNoAsc(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public StepAssignment getOne(UUID programId, UUID id) {
        return stepRepo.findByIdAndProgramId(id, programId)
                .orElseThrow(() -> notFound("StepAssignment", id));
    }

    @Override
    public StepAssignment create(UUID programId, CreateStepAssignmentReq req) {
        // 1) Đảm bảo Program tồn tại (nếu không cần dùng Program, chỉ để verify)
        programRepo.findById(programId)
                .orElseThrow(() -> notFound("Program", programId));

        // 2) (Tuỳ chọn) Kiểm tra trùng stepNo trong cùng program
        // -> cần có method tương ứng trong repository
        if (stepRepo.existsByProgramIdAndStepNo(programId, req.stepNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "stepNo đã tồn tại trong chương trình này");
        }

        // 3) Tạo entity: KHÔNG dùng setProgram(...), chỉ gán programId
        // plannedDay: nếu business = ngày N trong kế hoạch, có thể đặt = stepNo
        StepAssignment sa = StepAssignment.builder()
                .programId(programId)
                .stepNo(req.stepNo())
                .plannedDay(req.stepNo())        // hoặc tính theo rule khác của bạn
                .note(req.note())                // note là optional
                .scheduledAt(req.eventAt())      // nếu null, có thể để null hoặc tự tính sau
                .status(StepStatus.PENDING)      // có default rồi, nhưng set rõ ràng cũng ok
                .build();

        return stepRepo.save(sa);
    }



    @Override
    @Transactional
    public void updateStatus(UUID userId,
                             UUID programId,
                             UUID assignmentId,
                             StepStatus status,
                             String note) {

        // 1) Đảm bảo program thuộc về user
        Program program = programRepo
                .findByIdAndUserId(programId, userId)
                .orElseThrow(() -> new NotFoundException("Program not found"));

        // 2) Lấy step trong đúng program
        StepAssignment sa = stepRepo
                .findByIdAndProgramId(assignmentId, program.getId())
                .orElseThrow(() -> new NotFoundException("Step assignment not found"));
        if (status == StepStatus.COMPLETED) {
            long unfinishedBefore = stepRepo
                    .countByProgramIdAndStepNoLessThanAndStatusNot(
                            programId,
                            sa.getStepNo(),
                            StepStatus.COMPLETED
                    );

            if (unfinishedBefore > 0) {
                // ValidationException đang được RestExceptionHandler map sang 400
                throw new ValidationException(
                        "Bạn cần hoàn thành các bước trước đó trước khi đánh dấu bước này COMPLETE"
                );
            }
        }
        // 3) Cập nhật trạng thái
        sa.setStatus(status);
        sa.setNote(note);
        sa.setUpdatedAt(Instant.now());

        stepRepo.save(sa);
        recomputeProgramStatus(programId);
    }
    private void recomputeProgramStatus(UUID programId) {
        // Đếm tất cả step KHÔNG COMPLETED trong program này
        long remaining = stepRepo
                .countByProgramIdAndStatusNot(programId, StepStatus.COMPLETED);

        Program program = programRepo.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found"));

        if (remaining == 0L) {
            // Toàn bộ step đã COMPLETED → Program hoàn thành
            if (program.getStatus() != ProgramStatus.COMPLETED) {
                program.setStatus(ProgramStatus.COMPLETED);
                // Khi bạn bổ sung field completedAt / endDate thì set luôn tại đây
                programRepo.save(program);
            }
        } else {
            // Còn step chưa COMPLETED
            // Trường hợp bạn cho phép “mở lại” chương trình:
            if (program.getStatus() == ProgramStatus.COMPLETED) {
                program.setStatus(ProgramStatus.ACTIVE);
                programRepo.save(program);
            }
        }
    }

    @Override
    public void delete(UUID programId, UUID id) {
        // Đảm bảo chỉ xóa step thuộc đúng program
        StepAssignment exist = stepRepo.findByIdAndProgramId(id, programId)
                .orElseThrow(() -> notFound("StepAssignment", id));
        stepRepo.deleteByIdAndProgramId(exist.getId(), programId);
    }

    // Helper
    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static ResponseStatusException notFound(String what, UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found: " + id);
    }
    @Override
    @Transactional
    public void createForProgramFromTemplate(Program program, PlanTemplate template) {
        // 1. Lấy toàn bộ PlanStep của template (đã có thứ tự)
        List<PlanStep> steps = planStepRepo
                .findByTemplateIdOrderByDayNoAscSlotAsc(template.getId());

        // 2. Tính mốc thời gian bắt đầu (UTC)
        Instant startInstant = program.getTrialStartedAt();
        if (startInstant == null) {
            startInstant = program.getCreatedAt();
        }
        OffsetDateTime start = OffsetDateTime.ofInstant(startInstant, ZoneOffset.UTC);

        int stepNo = 1;
        List<StepAssignment> assignments = new ArrayList<>(steps.size());

        for (PlanStep ps : steps) {
            Integer dayNo = ps.getDayNo();
            if (dayNo == null) {
                throw new IllegalStateException(
                        "PlanStep.dayNo is null for step id " + ps.getId()
                );
            }

            StepAssignment sa = new StepAssignment();
            // id, createdAt, updatedAt sẽ được set trong @PrePersist
            sa.setProgramId(program.getId());
            sa.setStepNo(stepNo++);
            sa.setPlannedDay(dayNo);
            sa.setStatus(StepStatus.PENDING);

            // scheduledAt = start + (dayNo - 1) ngày, UTC
            sa.setScheduledAt(start.plusDays(dayNo - 1L));

            // completedAt, note, createdBy để null ban đầu
            assignments.add(sa);
        }

        stepRepo.saveAll(assignments);
    }

}
