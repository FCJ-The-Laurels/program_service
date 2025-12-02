package com.smokefree.program.web.dto.quiz.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO cập nhật nội dung quiz (chỉ thay đổi câu hỏi/lựa chọn).
 */
public record UpdateQuizContentReq(
        @Valid @NotEmpty List<QuestionDto> questions
) {}
