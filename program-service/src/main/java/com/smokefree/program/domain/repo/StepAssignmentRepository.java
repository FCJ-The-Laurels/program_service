package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.StepAssignment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StepAssignmentRepository extends JpaRepository<StepAssignment, UUID> {
    boolean existsByProgramIdAndStepNo(UUID programId, Integer stepNo);

    // Nếu StepAssignment có field createdAt:
    List<StepAssignment> findByProgramIdOrderByCreatedAtDesc(UUID programId);

    // Hoặc an toàn hơn: truyền Sort (tránh sai field)
    // List<StepAssignment> findByProgramId(UUID programId, Sort sort);
}
