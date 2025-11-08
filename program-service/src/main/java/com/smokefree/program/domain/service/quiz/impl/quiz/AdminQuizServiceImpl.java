package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizChoiceLabelRepository;
import com.smokefree.program.domain.repo.QuizTemplateQuestionRepository;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminQuizServiceImpl implements AdminQuizService {

    private final QuizTemplateRepository tplRepo;
    private final QuizTemplateQuestionRepository qRepo;
    private final QuizChoiceLabelRepository cRepo;

    @Override
    public QuizTemplate createTemplate(String name) {
        QuizTemplate t = QuizTemplate.builder()
                .name(name)
                .status(QuizTemplateStatus.DRAFT)
                .ownerType(QuizTemplateOwnerType.SYSTEM)
                .build();
        return tplRepo.save(t);
    }

    @Override public void publishTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setStatus(QuizTemplateStatus.PUBLISHED);
        tplRepo.save(t);
    }

    @Override
    public UUID addQuestion(UUID templateId, Integer orderNo, String text, String type, Integer points, String explanation) {
        QuizTemplateQuestion q = new QuizTemplateQuestion();
        q.setTemplateId(templateId);
        q.setOrderNo(orderNo);
        q.setQuestionText(text);
        q.setType(QuestionType.valueOf(type));
        q.setPoints(points);
        q.setExplanation(explanation);
        return qRepo.save(q).getId();
    }

    @Override
    public UUID addChoice(UUID questionId, String labelCode, String labelText, boolean isCorrect, Integer weight) {
        QuizChoiceLabel c = new QuizChoiceLabel();
        c.setQuestionId(questionId);
        c.setLabelCode(labelCode);
        c.setLabelText(labelText);
        c.setCorrect(Boolean.TRUE.equals(isCorrect));
        c.setWeight(weight);
        return cRepo.save(c).getId();
    }
}

