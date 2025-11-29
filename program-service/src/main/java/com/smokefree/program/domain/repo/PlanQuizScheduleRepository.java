package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.PlanQuizSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanQuizScheduleRepository extends JpaRepository<PlanQuizSchedule, UUID> {

    List<PlanQuizSchedule> findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(UUID planTemplateId);
}
