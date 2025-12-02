package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.PlanTemplate;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.dto.step.RescheduleStepReq;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StepAssignmentService {
    List<StepAssignment> listByProgram(UUID programId);
    List<StepAssignment> listByProgramAndDate(UUID programId, LocalDate date);
    StepAssignment getOne(UUID programId, UUID id);
    StepAssignment create(UUID programId, CreateStepAssignmentReq req);
    void updateStatus(UUID userId, UUID programId, UUID assignmentId, StepStatus status, String note);
    StepAssignment reschedule(UUID programId, UUID assignmentId, OffsetDateTime newScheduledAt);
    void delete(UUID programId, UUID id);
    void createForProgramFromTemplate(Program program, PlanTemplate template);

    /**
     * Tạo một nhiệm vụ phục hồi streak đặc biệt.
     * @param programId ID của chương trình
     * @param moduleCode Mã của module nội dung sẽ được gán
     * @param streakBreakId ID của bản ghi StreakBreak mà nhiệm vụ này đang sửa chữa
     * @return StepAssignment đã được tạo
     */
    StepAssignment createStreakRecoveryTask(UUID programId, String moduleCode, UUID streakBreakId);
}
