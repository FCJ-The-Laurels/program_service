package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.QuizAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, UUID> {
    List<QuizAssignment> findByProgramId(UUID programId);

    List<QuizAssignment> findByProgramIdAndActive(UUID programId, boolean active);

    boolean existsByTemplateIdAndProgramId(UUID templateId, UUID programId);

    @Query("SELECT qa FROM QuizAssignment qa WHERE qa.programId = :programId AND qa.active = true ORDER BY qa.startOffsetDay ASC")
    List<QuizAssignment> findActiveSortedByStartOffset(UUID programId);
}
