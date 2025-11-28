package com.smokefree.program.web.dto.program;

import com.smokefree.program.domain.model.ProgramStatus;

import java.util.UUID;

public record ProgramProgressRes(
        UUID programId,
        ProgramStatus status,
        int currentDay,
        int planDays,
        double percentComplete,
        int daysRemaining,
        int stepsCompleted,
        int stepsTotal,
        int streakCurrent,
        Integer trialRemainingDays
) {}

