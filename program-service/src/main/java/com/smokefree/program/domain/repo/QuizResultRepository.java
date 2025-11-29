package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizResultRepository extends JpaRepository<QuizResult, UUID> {
    Optional<QuizResult> findFirstByProgramIdAndTemplateIdOrderByCreatedAtDesc(UUID programId, UUID templateId);

    boolean existsByProgramIdAndTemplateId(UUID programId, UUID templateId);
}
