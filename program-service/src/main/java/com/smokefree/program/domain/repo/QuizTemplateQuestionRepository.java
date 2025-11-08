package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizTemplateQuestion;
import com.smokefree.program.domain.model.QuizTemplateQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizTemplateQuestionRepository
        extends JpaRepository<QuizTemplateQuestion, QuizTemplateQuestionId> {

    // Spring Data: truy cập field lồng qua tên "Id<Prop>"
    List<QuizTemplateQuestion> findByIdTemplateIdOrderByIdQuestionNoAsc(UUID templateId);
}
