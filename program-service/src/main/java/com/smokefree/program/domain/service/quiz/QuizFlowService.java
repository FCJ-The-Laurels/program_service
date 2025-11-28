package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.web.dto.quiz.answer.AnswerReq;
import com.smokefree.program.web.dto.quiz.attempt.DueItem;
import com.smokefree.program.web.dto.quiz.attempt.OpenAttemptRes;
import com.smokefree.program.web.dto.quiz.result.SubmitRes;

import java.util.List;
import java.util.UUID;

public interface QuizFlowService {
    List<DueItem> listDue(UUID userId);
    OpenAttemptRes openAttempt(UUID userId, UUID templateId);
    void saveAnswer(UUID userId, UUID attemptId, AnswerReq req);
    SubmitRes submit(UUID userId, UUID attemptId);
}
