package com.smokefree.program.domain.service.template;


import com.smokefree.program.web.dto.plan.PlanDaysRes;
import com.smokefree.program.web.dto.plan.PlanRecommendationRes;
import com.smokefree.program.web.dto.plan.PlanTemplateDetailRes;
import com.smokefree.program.web.dto.plan.PlanTemplateSummaryRes;

import java.util.List;
import java.util.UUID;

public interface PlanTemplateService {
    List<PlanTemplateSummaryRes> listAll();
    PlanTemplateDetailRes getDetail(UUID id);
    PlanRecommendationRes recommendBySeverity(String severity);

    PlanDaysRes getDays(UUID templateId, boolean expandModule, String lang);
    PlanDaysRes getDaysByCode(String code, boolean expandModule, String lang);
}

