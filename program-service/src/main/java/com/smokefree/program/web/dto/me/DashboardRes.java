package com.smokefree.program.web.dto.me;

import java.util.List;
import java.util.UUID;

public record DashboardRes(
        UUID userId,
        SubscriptionRes subscription,
        ActiveProgramRes activeProgram,
        List<DueQuizRes> dueQuizzes,
        StreakInfoRes streakInfo
) {}

