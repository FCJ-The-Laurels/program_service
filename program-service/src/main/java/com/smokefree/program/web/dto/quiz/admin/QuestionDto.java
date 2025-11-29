package com.smokefree.program.web.dto.quiz.admin;

import com.smokefree.program.domain.model.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record QuestionDto(
        Integer orderNo,
        String questionText,
        QuestionType type,
        String explanation,
        @Valid @NotEmpty List<ChoiceDto> choices
) {}