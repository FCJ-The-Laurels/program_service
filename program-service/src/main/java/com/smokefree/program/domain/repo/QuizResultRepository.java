package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizResultRepository extends JpaRepository<QuizResult, UUID> {
    Optional<QuizResult> findFirstByProgramIdAndTemplateIdOrderByCreatedAtDesc(UUID programId, UUID templateId);

    java.util.List<QuizResult> findByProgramId(UUID programId);

    @org.springframework.data.jpa.repository.Query("SELECT qr FROM QuizResult qr WHERE qr.programId = :programId ORDER BY qr.createdAt DESC LIMIT :limit")
    java.util.List<QuizResult> findRecentByProgramId(UUID programId, int limit);

    boolean existsByProgramIdAndTemplateId(UUID programId, UUID templateId);
}
