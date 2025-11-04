package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "step_assignments", schema = "program")
public class StepAssignment {
    @Id
    private UUID id;

    @Column(name="program_id", nullable=false)
    private UUID programId;

    @Column(name="step_no", nullable=false)
    private Integer stepNo;        // 1..N

    @Column(name="planned_day", nullable=false)
    private Integer plannedDay;    // ngày N trong plan

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private StepStatus status = StepStatus.PENDING;

    @Column(name="scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name="completed_at")
    private OffsetDateTime completedAt;

    @Column(name="note")
    private String note;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="created_by")
    private UUID createdBy;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void preInsert() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
