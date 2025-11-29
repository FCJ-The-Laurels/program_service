package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;

import java.util.UUID;

public interface AdminQuizService {

    /**
     * Tạo một Quiz Template hoàn chỉnh từ DTO.
     */
    QuizTemplate createFullQuiz(CreateFullQuizReq req);

    /**
     * Cập nhật toàn bộ nội dung của một Quiz Template từ DTO.
     */
    void updateFullQuiz(UUID templateId, UpdateFullQuizReq req);

    /**
     * Xuất bản một quiz template.
     */
    void publishTemplate(UUID templateId);

    /**
     * Lưu trữ một quiz template.
     */
    void archiveTemplate(UUID templateId);
}
