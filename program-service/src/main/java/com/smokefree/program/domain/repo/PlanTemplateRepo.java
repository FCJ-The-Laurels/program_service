// src/main/java/com/smokefree/program/domain/repo/PlanTemplateRepo.java
package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.PlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PlanTemplateRepo extends JpaRepository<PlanTemplate, UUID> {

    Optional<PlanTemplate> findByCode(String code);

    Optional<PlanTemplate> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    @Query("select t from PlanTemplate t order by t.level asc, t.code asc")
    List<PlanTemplate> findAllOrderByLevelCode();

    // Tiện nếu bạn chỉ cần id theo code (giảm payload)
    @Query("select t.id from PlanTemplate t where lower(t.code) = lower(?1)")
    Optional<UUID> findIdByCode(String code);

}
