package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;

import java.util.List;
import java.util.UUID;

public interface AdminQuizService {

    QuizTemplate createFullQuiz(CreateFullQuizReq req);

    void updateFullQuiz(UUID templateId, UpdateFullQuizReq req);

    void publishTemplate(UUID templateId);

    void archiveTemplate(UUID templateId);

    List<QuizTemplate> listAll();

    QuizTemplate getDetail(UUID templateId);

    void deleteTemplate(UUID templateId);

    void revertToDraft(UUID templateId);
    /**
     * Cập nhật lại nội dung (câu hỏi/lựa chọn), giữ nguyên metadata.
     */
    void updateContent(UUID templateId, com.smokefree.program.web.dto.quiz.admin.UpdateQuizContentReq req);
}
