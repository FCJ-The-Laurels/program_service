package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;

import java.util.List;
import java.util.UUID;

public interface StepAssignmentService {
    List<StepAssignment> listByProgram(UUID programId);
    StepAssignment getOne(UUID programId, UUID id);
    StepAssignment create(UUID programId, CreateStepAssignmentReq req);
    void updateStatus(UUID userId,
                      UUID programId,
                      UUID assignmentId,
                      StepStatus status,
                      String note);
    void delete(UUID programId, UUID id);

}
