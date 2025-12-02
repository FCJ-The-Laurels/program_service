package com.smokefree.program.web.dto.quiz.admin;

import java.util.List;
import java.util.UUID;

public record QuizTemplateDetailRes(
        UUID id,
        String code,
        String name,
        Integer version,
        String status,
        List<QuestionDto> questions
) {}
