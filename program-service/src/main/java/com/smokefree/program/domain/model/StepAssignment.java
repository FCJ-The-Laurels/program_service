package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "step_assignments", schema = "program")
public class StepAssignment {

    public enum AssignmentType {
        REGULAR,
        STREAK_RECOVERY
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID programId;

    @Column(nullable = false)
    private Integer stepNo;

    private Integer plannedDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StepStatus status;

    private OffsetDateTime scheduledAt;
    private OffsetDateTime completedAt;
    private String note;

    @Column(nullable = false)
    private Instant createdAt;
    private UUID createdBy;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "module_code")
    private String moduleCode;

    @Column(name = "module_version")
    private String moduleVersion;

    @Column(name = "title_override")
    private String titleOverride;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AssignmentType assignmentType = AssignmentType.REGULAR;

    @Column(name = "streak_break_id")
    private UUID streakBreakId;

    @PrePersist
    void preInsert() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
