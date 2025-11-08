// Thông tin một lần gãy streak
package com.smokefree.program.web.dto.streak;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StreakBreakRes(
        UUID id,
        UUID programId,
        UUID streakId,
        UUID smokeEventId,
        OffsetDateTime brokenAt,
        Integer prevStreakDays,
        String note,
        Instant createdAt
) {}
