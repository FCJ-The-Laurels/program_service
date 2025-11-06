// com/smokefree/program/web/dto/plan/PlanDayRes.java
package com.smokefree.program.web.dto.plan;

import java.util.List;

public record PlanDayRes(
        Integer dayNo,
        List<PlanStepRes> steps
) {}
