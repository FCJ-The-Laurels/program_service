package com.smokefree.program.web.dto.me;

public record StreakInfoRes(
        Integer currentStreak,
        Integer longestStreak,
        Integer daysWithoutSmoke
) {}

