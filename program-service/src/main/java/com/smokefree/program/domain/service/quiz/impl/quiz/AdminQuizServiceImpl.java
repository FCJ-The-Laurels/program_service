package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import com.smokefree.program.web.dto.quiz.admin.ChoiceDto;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.QuestionDto;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminQuizServiceImpl implements AdminQuizService {

    private final QuizTemplateRepository tplRepo;

    @Override
    public QuizTemplate createFullQuiz(CreateFullQuizReq req) {
        // 1. Tạo đối tượng QuizTemplate cha
        QuizTemplate template = new QuizTemplate();
        template.setName(req.name());
        template.setVersion(req.version() != null ? req.version() : 1);
        // Các giá trị mặc định khác sẽ được set bởi @PrePersist trong Entity

        // 2. Lặp qua DTO câu hỏi và xây dựng các Entity con
        for (QuestionDto questionDto : req.questions()) {
            QuizTemplateQuestion newQuestion = new QuizTemplateQuestion();
            newQuestion.setId(new QuizTemplateQuestionId(template.getId(), questionDto.orderNo()));
            newQuestion.setTemplate(template);
            newQuestion.setQuestionText(questionDto.questionText());
            newQuestion.setType(questionDto.type());
            newQuestion.setExplanation(questionDto.explanation());

            // 3. Lặp qua DTO lựa chọn và xây dựng các Entity cháu
            for (ChoiceDto choiceDto : questionDto.choices()) {
                QuizChoiceLabel newChoice = new QuizChoiceLabel();
                newChoice.setId(new QuizChoiceLabelId(template.getId(), questionDto.orderNo(), choiceDto.labelCode()));
                newChoice.setQuestion(newQuestion);
                newChoice.setLabelText(choiceDto.labelText());
                newChoice.setCorrect(choiceDto.isCorrect());
                newChoice.setWeight(choiceDto.weight());
                
                newQuestion.getChoiceLabels().add(newChoice);
            }
            template.getQuestions().add(newQuestion);
        }

        // 4. Lưu đối tượng cha. Do có CascadeType.ALL, tất cả các con và cháu sẽ được lưu theo.
        return tplRepo.save(template);
    }

    @Override
    public void updateFullQuiz(UUID templateId, UpdateFullQuizReq req) {
        QuizTemplate template = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + templateId));

        Assert.isTrue(template.getStatus() == QuizTemplateStatus.DRAFT, "Can only update a DRAFT template.");

        if (req.name() != null && !req.name().isBlank()) {
            template.setName(req.name());
        }
        if (req.version() != null) {
            template.setVersion(req.version());
        }

        template.getQuestions().clear();
        tplRepo.flush(); 

        for (QuestionDto questionDto : req.questions()) {
            QuizTemplateQuestion newQuestion = new QuizTemplateQuestion();
            newQuestion.setId(new QuizTemplateQuestionId(templateId, questionDto.orderNo()));
            newQuestion.setTemplate(template);
            newQuestion.setQuestionText(questionDto.questionText());
            newQuestion.setType(questionDto.type());
            newQuestion.setExplanation(questionDto.explanation());

            for (ChoiceDto choiceDto : questionDto.choices()) {
                QuizChoiceLabel newChoice = new QuizChoiceLabel();
                newChoice.setId(new QuizChoiceLabelId(templateId, questionDto.orderNo(), choiceDto.labelCode()));
                newChoice.setQuestion(newQuestion);
                newChoice.setLabelText(choiceDto.labelText());
                newChoice.setCorrect(choiceDto.isCorrect());
                newChoice.setWeight(choiceDto.weight());
                
                newQuestion.getChoiceLabels().add(newChoice);
            }
            template.getQuestions().add(newQuestion);
        }

        template.setUpdatedAt(Instant.now());
        tplRepo.save(template);
    }

    @Override
    public void publishTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setStatus(QuizTemplateStatus.PUBLISHED);
        t.setPublishedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }

    @Override
    public void archiveTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setStatus(QuizTemplateStatus.ARCHIVED);
        t.setArchivedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }
}
