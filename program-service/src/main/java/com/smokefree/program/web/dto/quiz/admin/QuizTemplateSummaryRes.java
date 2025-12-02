package com.smokefree.program.web.dto.quiz.admin;

import java.time.Instant;
import java.util.UUID;

public record QuizTemplateSummaryRes(
        UUID id,
        String code,
        String name,
        Integer version,
        String status,
        int questionCount,
        Instant createdAt
) {}
