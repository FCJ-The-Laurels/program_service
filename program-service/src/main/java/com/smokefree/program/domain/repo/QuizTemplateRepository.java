package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.domain.model.QuizTemplateOwnerType;
import com.smokefree.program.domain.model.QuizTemplateStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizTemplateRepository extends JpaRepository<QuizTemplate, UUID> {
    @EntityGraph(attributePaths = {"questions", "questions.choices"})
    Optional<QuizTemplate> findById(UUID id);
    List<QuizTemplate> findByOwnerTypeAndOwnerId(QuizTemplateOwnerType ownerType, UUID ownerId);
}