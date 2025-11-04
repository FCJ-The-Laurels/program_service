package com.smokefree.program.domain.service.smoke;

import java.util.UUID;

public interface StepAssignmentService {
    void onStreakBreak(UUID programId, java.time.OffsetDateTime brokeAt);

}
