// File: web/dto/quiz/admin/ChoiceDto.java
package com.smokefree.program.web.dto.quiz.admin;

public record ChoiceDto(
        String labelCode,
        String labelText,
        boolean isCorrect,
        Integer weight
) {}