package com.smokefree.program.domain.service.quiz;



import com.smokefree.program.domain.model.QuizTemplate;

import java.util.UUID;

public interface AdminQuizService {
    QuizTemplate createTemplate(String name);
    void publishTemplate(UUID templateId);
    UUID addQuestion(UUID templateId, Integer orderNo, String text, String type, Integer points, String explanation);
    UUID addChoice(UUID questionId, String labelCode, String labelText, boolean isCorrect, Integer weight);
}
