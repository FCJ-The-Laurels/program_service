package com.smokefree.program.web.dto.quiz.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateFullQuizReq(
        String name,
        Integer version,
        @Valid @NotEmpty List<QuestionDto> questions
) {}