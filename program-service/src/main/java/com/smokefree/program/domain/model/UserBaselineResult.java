package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Lưu kết quả quiz onboarding (baseline) của user trước khi tạo Program.
 */
@Entity
@Table(name = "user_baseline_results", schema = "program",
        indexes = {
                @Index(name = "idx_user_baseline_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_baseline_user", columnNames = {"user_id"})
        })
@Getter
@Setter
public class UserBaselineResult {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "quiz_template_id", nullable = false)
    private UUID quizTemplateId;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "severity", nullable = false)
    private SeverityLevel severity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
