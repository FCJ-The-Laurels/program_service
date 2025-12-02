package com.smokefree.program.domain.service.onboarding;

import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.model.UserBaselineResult;

import java.util.Optional;
import java.util.UUID;

public interface BaselineResultService {
    UserBaselineResult saveOrUpdate(UUID userId, UUID quizTemplateId, int totalScore, SeverityLevel severity);
    Optional<UserBaselineResult> latest(UUID userId);
    boolean hasBaseline(UUID userId);
}
