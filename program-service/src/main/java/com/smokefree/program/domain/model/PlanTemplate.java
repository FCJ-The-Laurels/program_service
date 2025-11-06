// src/main/java/com/smokefree/program/domain/model/PlanTemplate.java
package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "plan_templates", schema = "program")
public class PlanTemplate {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private Integer level;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    // ---- CHỈ 1 PrePersist ----
    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
