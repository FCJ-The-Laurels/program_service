package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.PlanTemplate;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StepAssignmentService {
    List<StepAssignment> listByProgram(UUID programId);
    List<StepAssignment> listByProgramAndDate(UUID programId, LocalDate date);
    StepAssignment getOne(UUID programId, UUID id);
    StepAssignment create(UUID programId, CreateStepAssignmentReq req);
    void updateStatus(UUID userId,
                      UUID programId,
                      UUID assignmentId,
                      StepStatus status,
                      String note);

    /**
     * Reschedule a step assignment to a new scheduled date/time with validation and access checks.
     */
    StepAssignment reschedule(UUID programId, UUID assignmentId, java.time.OffsetDateTime newScheduledAt);
    void delete(UUID programId, UUID id);
    void createForProgramFromTemplate(Program program, PlanTemplate template);
}
