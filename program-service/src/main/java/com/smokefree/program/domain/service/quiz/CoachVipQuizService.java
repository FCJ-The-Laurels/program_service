package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.web.dto.quiz.coach.CloneForVipRes;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CoachVipQuizService {
    CloneForVipRes cloneForUserAndAssign(UUID programId, UUID userId, UUID baseTemplateId,
                                         String newName, OffsetDateTime dueAt, String note);
}