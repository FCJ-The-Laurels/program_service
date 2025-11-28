package com.smokefree.program.web.dto.me;

import java.time.Instant;
import java.util.UUID;

public record ActiveProgramRes(
        UUID id,
        String templateCode,
        String templateName,
        String status,
        int currentDay,
        int totalDays,
        boolean trial,
        Integer trialRemainingDays,
        Instant createdAt
) {}

