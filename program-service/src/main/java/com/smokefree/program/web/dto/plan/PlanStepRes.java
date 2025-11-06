// com/smokefree/program/web/dto/plan/PlanStepRes.java
package com.smokefree.program.web.dto.plan;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.Map;

public record PlanStepRes(
        String slot,
        String title,
        Integer maxMinutes,
        String type,          // lấy từ module.type nếu module_code != null
        String moduleCode,    // nullable
        ModuleBrief module    // nullable nếu expand=false
) {
    public record ModuleBrief(
            Integer version,
            String type,
            Map<String,Object> payload,
            String etag
    ) {}
}
