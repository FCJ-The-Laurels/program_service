package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizChoiceLabel;
import com.smokefree.program.domain.model.QuizChoiceLabelId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizChoiceLabelRepository
        extends JpaRepository<QuizChoiceLabel, QuizChoiceLabelId> {

    List<QuizChoiceLabel> findByIdTemplateIdAndIdQuestionNoOrderByIdLabelCodeAsc(UUID templateId, Integer questionNo);
}
