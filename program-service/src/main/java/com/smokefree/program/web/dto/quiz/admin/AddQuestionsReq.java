package com.smokefree.program.web.dto.quiz.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

// Sử dụng record cho DTO đơn giản
public record AddQuestionsReq(
        @Valid // Đảm bảo các đối tượng AddQuestionReq bên trong cũng được validate
        @NotEmpty // Đảm bảo danh sách không được rỗng
        List<AddQuestionReq> questions
) {}