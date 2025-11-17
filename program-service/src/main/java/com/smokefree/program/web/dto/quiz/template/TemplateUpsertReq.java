// com.smokefree.program.web.dto.quiz.template.TemplateUpsertReq
package com.smokefree.program.web.dto.quiz.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TemplateUpsertReq(
        @NotBlank String name,
        String languageCode,
        @Size(min = 1) List<QuestionUpsertReq> questions
) {}
