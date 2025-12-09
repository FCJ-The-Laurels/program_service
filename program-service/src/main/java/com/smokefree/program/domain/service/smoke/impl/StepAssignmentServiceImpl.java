package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.domain.repo.PlanStepRepo;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StepAssignmentServiceImpl implements StepAssignmentService {

    private final StepAssignmentRepository stepAssignmentRepository;
    private final PlanStepRepo planStepRepository;
    private final ProgramRepository programRepository;
    private final StreakService streakService;
    private final ContentModuleRepository contentModuleRepo;
    private final StreakRepository streakRepo; // Thêm dependency này

    /**
     * Tạo một nhiệm vụ phục hồi streak đặc biệt, được kích hoạt sau một sự kiện SLIP.
     */
    @Override
    @Transactional
    public StepAssignment createStreakRecoveryTask(UUID programId, String moduleCode, UUID streakBreakId) {
        ensureProgramAccess(programId, false);

        // 1. Tìm module nội dung được cấu hình cho việc phục hồi (với ngôn ngữ mặc định là 'vi')
        final String lang = "vi";
        ContentModule module = contentModuleRepo.findTopByCodeAndLangOrderByVersionDesc(moduleCode, lang)
                .orElseThrow(() -> new NotFoundException("Recovery content module not found with code: " + moduleCode + " and lang: " + lang));

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        // 2. Lấy tiêu đề từ payload của module một cách an toàn
        String title = "Nhiệm vụ Phục hồi Chuỗi"; // Tiêu đề mặc định
        if (module.getPayload() != null && module.getPayload().get("title") instanceof String) {
            title = (String) module.getPayload().get("title");
        }

        // 3. Tạo nhiệm vụ đặc biệt
        StepAssignment recoveryTask = StepAssignment.builder()
                .id(UUID.randomUUID())
                .programId(programId)
                .stepNo(999) // Dùng một số đặc biệt để dễ nhận biết trong DB, không ảnh hưởng logic
                .plannedDay(program.getCurrentDay())
                .status(StepStatus.PENDING)
                .assignmentType(StepAssignment.AssignmentType.STREAK_RECOVERY) // Đánh dấu đây là nhiệm vụ phục hồi
                .streakBreakId(streakBreakId) // Liên kết đến lần break mà nó đang sửa chữa
                .moduleCode(module.getCode())
                .moduleVersion(String.valueOf(module.getVersion()))
                .titleOverride(title)
                .scheduledAt(OffsetDateTime.now(ZoneOffset.UTC)) // Lên lịch cho ngay bây giờ
                .createdBy(program.getUserId())
                .build();

        log.info("Created streak recovery task '{}' for program {}", title, programId);
        return stepAssignmentRepository.save(recoveryTask);
    }

    /**
     * Cập nhật trạng thái của một step. Phân nhánh logic dựa trên loại nhiệm vụ.
     */
    @Override
    @Transactional
    public void updateStatus(UUID userId, UUID programId, UUID assignmentId, StepStatus status, String note) {
        ensureProgramAccess(programId, false);
        log.info("[StepAssignment] updateStatus: programId={}, stepId={}, status={}", programId, assignmentId, status);
        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(assignmentId, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + assignmentId));

        if (step.getStatus() == status) {
            return; // Không làm gì nếu trạng thái không đổi
        }

        step.setStatus(status);
        step.setNote(note);
        if (status == StepStatus.COMPLETED) {
            step.setCompletedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
        }
        stepAssignmentRepository.save(step);

        // Phân nhánh logic: Nếu là nhiệm vụ phục hồi thì phục hồi streak, nếu không thì xử lý ngày bình thường
        if (step.getAssignmentType() == StepAssignment.AssignmentType.STREAK_RECOVERY && status == StepStatus.COMPLETED) {
            log.info("Streak recovery task {} completed. Restoring streak.", step.getId());
            streakService.restoreStreak(step.getStreakBreakId());
        } else if (step.getAssignmentType() == StepAssignment.AssignmentType.REGULAR) {
            handleDayCompletion(programId, step.getPlannedDay());
        }
    }

    /**
     * Xử lý logic khi một ngày thông thường (REGULAR) được hoàn thành.
     * Đây là nơi logic cập nhật streak được triển khai.
     */
    /**
     * X? l� c?p nh?t streak khi ho�n th�nh h?t step c?a m?t ng�y, c� ch?n tru?ng h?p v?a c� smoke.
     */
    private void handleDayCompletion(UUID programId, int plannedDay) {
        long incompleteSteps = stepAssignmentRepository.countIncompleteStepsForDay(programId, plannedDay, StepStatus.COMPLETED);
        if (incompleteSteps > 0) {
            return;
        }

        log.info("[DayCompletion] All steps for day {} completed. Updating streak counts.", plannedDay, programId);

        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        LocalDate completedDate = program.getStartDate().plusDays(plannedDay - 1);
        if (program.getLastSmokeAt() != null) {
            LocalDate lastSmokeDate = program.getLastSmokeAt().toLocalDate();
            if (!lastSmokeDate.isBefore(completedDate)) {
                log.info("[DayCompletion] Skip streak update because last smoke {} is on/after {}.", lastSmokeDate, completedDate);
                return;
            }
        }

        Streak activeStreak = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId)
            .orElseGet(() -> {
                Streak newStreak = new Streak();
                newStreak.setProgramId(programId);
                newStreak.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
                return streakRepo.save(newStreak);
            });

        LocalDate startDate = activeStreak.getStartedAt().toLocalDate();
        long newStreakValue = ChronoUnit.DAYS.between(startDate, completedDate) + 1;

        program.setStreakCurrent((int) newStreakValue);
        if (program.getStreakCurrent() > program.getStreakBest()) {
            program.setStreakBest(program.getStreakCurrent());
        }

        programRepository.save(program);
        log.info("Successfully updated streak for program {}. New current: {}, New best: {}",
                 programId, program.getStreakCurrent(), program.getStreakBest());
    }

    @Deprecated   private void handleDayCompletionLegacy(UUID programId, int plannedDay) {
        long incompleteSteps = stepAssignmentRepository.countIncompleteStepsForDay(programId, plannedDay, StepStatus.COMPLETED);
        if (incompleteSteps == 0) {
            log.info("[DayCompletion] All steps for day {} completed. Updating streak counts.", plannedDay, programId);

            // Bước 1: Lấy Program và Streak để tính toán
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
            
            Streak activeStreak = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId)
                .orElseGet(() -> {
                    // Nếu không có streak, tạo mới và trả về ngay
                    Streak newStreak = new Streak(); // Sử dụng constructor mặc định
                    newStreak.setProgramId(programId);
                    newStreak.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC)); // Đặt startedAt
                    return streakRepo.save(newStreak);
                });

            // Bước 2: Tính toán số ngày streak mới
            LocalDate startDate = activeStreak.getStartedAt().toLocalDate();
            // Dùng plannedDay của step vừa hoàn thành để đảm bảo tính đúng ngày
            LocalDate completedDate = program.getStartDate().plusDays(plannedDay - 1); 
            
            long newStreakValue = ChronoUnit.DAYS.between(startDate, completedDate) + 1;

            // Bước 3: Cập nhật và lưu lại Program
            program.setStreakCurrent((int) newStreakValue);
            if (program.getStreakCurrent() > program.getStreakBest()) {
                program.setStreakBest(program.getStreakCurrent());
            }
            
            programRepository.save(program);
            log.info("Successfully updated streak for program {}. New current: {}, New best: {}",
                     programId, program.getStreakCurrent(), program.getStreakBest());
        }
    }
    
    // ... các phương thức còn lại không thay đổi ...
    @Override
    @Transactional(readOnly = true)
    public List<StepAssignment> listByProgram(UUID programId) {
        ensureProgramAccess(programId, true);
        return stepAssignmentRepository.findByProgramIdOrderByStepNoAsc(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<StepAssignment> listByProgram(UUID programId, int page, int size) {
        ensureProgramAccess(programId, true);
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                org.springframework.data.domain.Sort.by("stepNo").ascending());
        return stepAssignmentRepository.findByProgramId(programId, pageable);
    }

    @Override
    @Transactional
    public List<StepAssignment> listByProgramAndDate(UUID programId, LocalDate date) {
        ensureProgramAccess(programId, true);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
        if (program.getStartDate() == null) {
            return List.of();
        }
        long offset = ChronoUnit.DAYS.between(program.getStartDate(), date);
        int plannedDay = (int) offset + 1;

        // Auto-update Current Day if we moved to a new day
        if (plannedDay > program.getCurrentDay() && plannedDay <= program.getPlanDays()) {
            log.info("[AutoUpdate] Updating currentDay from {} to {} for program {}",
                     program.getCurrentDay(), plannedDay, programId);
            program.setCurrentDay(plannedDay);
            programRepository.save(program);
        }

        if (plannedDay < 1) {
            return List.of();
        }
        return stepAssignmentRepository.findByProgramIdAndPlannedDay(programId, plannedDay);
    }

    @Override
    @Transactional(readOnly = true)
    public StepAssignment getOne(UUID programId, UUID id) {
        ensureProgramAccess(programId, true);
        return stepAssignmentRepository.findByIdAndProgramId(id, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + id));
    }

    @Override
    @Transactional
    public StepAssignment create(UUID programId, CreateStepAssignmentReq req) {
        ensureProgramAccess(programId, false);
        StepAssignment assignment = StepAssignment.builder()
            .id(UUID.randomUUID())
            .programId(programId)
            .stepNo(req.stepNo())
            .plannedDay(req.plannedDay())
            .status(StepStatus.PENDING)
            .createdBy(SecurityUtil.requireUserId())
            .build();
        return stepAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public StepAssignment reschedule(UUID programId, UUID assignmentId, OffsetDateTime newScheduledAt) {
        ensureProgramAccess(programId, false);
        if (newScheduledAt == null) {
            throw new IllegalArgumentException("newScheduledAt is required");
        }
        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(assignmentId, programId)
                .orElseThrow(() -> new NotFoundException("Step not found: " + assignmentId));
        step.setScheduledAt(newScheduledAt);
        return stepAssignmentRepository.save(step);
    }

    @Override
    @Transactional
    public void delete(UUID programId, UUID id) {
        ensureProgramAccess(programId, false);
        StepAssignment step = stepAssignmentRepository.findByIdAndProgramId(id, programId)
            .orElseThrow(() -> new NotFoundException("Step not found: " + id));
        stepAssignmentRepository.delete(step);
    }

    @Override
    @Transactional
    public void createForProgramFromTemplate(Program program, PlanTemplate template) {
        List<PlanStep> templateSteps = planStepRepository.findByTemplateIdOrderByDayNoAscSlotAsc(template.getId());
        log.info("[StepAssignment] Creating {} step assignments for program: {}", templateSteps.size(), program.getId());
        int stepNo = 1;
        for (PlanStep planStep : templateSteps) {
            StepAssignment assignment = StepAssignment.builder()
                .id(UUID.randomUUID())
                .programId(program.getId())
                .stepNo(stepNo++)
                .plannedDay(planStep.getDayNo())
                .status(StepStatus.PENDING)
                .assignmentType(StepAssignment.AssignmentType.REGULAR)
                .createdBy(program.getUserId())
                .build();
            LocalDate startDate = program.getStartDate();
            LocalDate scheduledDate = startDate.plusDays(planStep.getDayNo() - 1);
            assignment.setScheduledAt(scheduledDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime());
            stepAssignmentRepository.save(assignment);
        }
    }

    private void ensureProgramAccess(UUID programId, boolean allowCoachWrite) {
        if (SecurityUtil.hasRole("ADMIN")) {
            return;
        }
        UUID userId = SecurityUtil.requireUserId();
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        // Chặn khi hết trial
        if (program.getTrialEndExpected() != null && java.time.Instant.now().isAfter(program.getTrialEndExpected())) {
            throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        boolean isOwner = program.getUserId().equals(userId);
        boolean isCoach = program.getCoachId() != null && program.getCoachId().equals(userId) && SecurityUtil.hasRole("COACH");
        if (!isOwner && !isCoach) {
            throw new ForbiddenException("Access denied for program " + programId);
        }
        if (isCoach && !allowCoachWrite) {
            throw new ForbiddenException("Coach cannot modify steps for program " + programId);
        }
    }
}


