package com.smokefree.program.web.dto.quiz.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smokefree.program.domain.model.QuestionType;
import jakarta.validation.constraints.*;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionUpsertReq(
        @Positive Integer questionNo,     // cho phép null để auto-đánh số
        @NotBlank String text,
        @NotNull QuestionType type,
        @PositiveOrZero Integer points,
        String explanation,
        @Size(min = 1) List<ChoiceUpsertReq> choices
) {}
