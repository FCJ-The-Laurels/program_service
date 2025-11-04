package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.web.dto.streak.StreakView;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface StreakService {
    StreakView start(UUID programId, OffsetDateTime startedAt);
    void breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String reason);
    StreakView current(UUID programId);
}
