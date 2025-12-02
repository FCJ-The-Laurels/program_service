package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quiz_assignments", schema = "program")
@Getter
@Setter
public class QuizAssignment {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    @Column(name = "period_days")
    private Integer periodDays;

    @Column(name = "start_offset_day")
    private Integer startOffsetDay;

    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "use_latest_version", nullable = false)
    private boolean useLatestVersion = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    // DB: every_days integer DEFAULT 5 NOT NULL
    @Column(name = "every_days", nullable = false)
    private Integer everyDays = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AssignmentScope scope = AssignmentScope.DAY;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizAssignmentOrigin origin = QuizAssignmentOrigin.MANUAL;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (everyDays == null) {
            everyDays = 5;
        }
        if (scope == null) {
            scope = AssignmentScope.DAY;
        }
        if (origin == null) {
            origin = QuizAssignmentOrigin.MANUAL;
        }
    }
}
