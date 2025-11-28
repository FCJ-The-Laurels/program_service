package com.smokefree.program.web.dto.quiz.template;

import jakarta.validation.constraints.NotBlank;

public record CreateTemplateReq(
    @NotBlank(message = "Template name is required")
    String name
) {}