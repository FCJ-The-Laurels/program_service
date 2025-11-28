package com.smokefree.program.web.dto.smoke;

import java.util.List;

public record SmokeEventStatisticsRes(
        int totalCount,
        int weekCount,
        double avgPerDay,
        List<String> trend
) {}

