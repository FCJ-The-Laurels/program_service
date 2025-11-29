package com.smokefree.program.web.dto.quiz.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateFullQuizReq(
    @NotBlank String name,
    Integer version, // Tùy chọn, sẽ mặc định là 1 nếu không có
    @Valid @NotEmpty List<QuestionDto> questions
) {}
