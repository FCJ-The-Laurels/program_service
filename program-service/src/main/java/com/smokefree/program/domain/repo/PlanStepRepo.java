// com/smokefree/program/domain/repo/PlanStepRepo.java
package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.PlanStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanStepRepo extends JpaRepository<PlanStep, UUID> {
    List<PlanStep> findByTemplateIdOrderByDayNoAscSlotAsc(UUID templateId);
//    List<PlanStep> findByTemplateIdOrderByDayNoAscSlotNoAsc(UUID templateId);
}
