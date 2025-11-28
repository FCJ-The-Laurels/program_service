package com.smokefree.program.web.dto.program;

import java.time.Instant;

public record TrialStatusRes(
        boolean isTrialPeriod,
        Instant startedAt,
        Instant expiresAt,
        Integer remainingDays,
        boolean canUpgradeNow
) {}

