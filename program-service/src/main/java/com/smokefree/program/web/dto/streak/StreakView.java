package com.smokefree.program.web.dto.streak;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StreakView(
        UUID streakId,
        int days,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {}
