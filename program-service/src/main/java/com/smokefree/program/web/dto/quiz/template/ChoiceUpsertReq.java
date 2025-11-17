package com.smokefree.program.web.dto.quiz.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChoiceUpsertReq(
        @NotBlank String labelCode,
        @NotBlank String labelText,
        Boolean correct,
        Integer weight
) {}
