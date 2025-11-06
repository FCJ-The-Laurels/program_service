// com/smokefree/program/web/dto/plan/PlanDaysRes.java
package com.smokefree.program.web.dto.plan;

import java.util.List;
import java.util.UUID;

public record PlanDaysRes(
        UUID id,
        String code,
        String name,
        Integer totalDays,
        List<PlanDayRes> days
) {}

