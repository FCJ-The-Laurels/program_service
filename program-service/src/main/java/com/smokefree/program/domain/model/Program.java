package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "programs", schema = "program")
public class Program {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    private UUID userId;
    public UUID coachId;

    private UUID chatroomId;

    @Column(nullable=false)
    private int planDays; // 30|45|60

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    @Column(nullable=false)
    private LocalDate startDate;
    @Column(nullable=false)
    private int currentDay = 1;

    private Integer totalScore;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SeverityLevel severity;

    // snapshot trial
    private String entitlementTierAtCreation;
    private Instant trialStartedAt;
    private Instant trialEndExpected;

    @Column(nullable=false)
    private Instant createdAt;

    @Column(nullable=false)
    private Instant updatedAt;

    private Instant deletedAt;

    @PrePersist void preInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (startDate == null) startDate = LocalDate.now(ZoneOffset.UTC);
    }
    @PreUpdate void preUpdate(){
        updatedAt = Instant.now();
    }

    @Column(name = "streak_current", nullable = false)
    private int streakCurrent = 0;

    @Column(name = "streak_best", nullable = false)
    private int streakBest = 0;

    @Column(name = "last_smoke_at")
    private OffsetDateTime lastSmokeAt;

    @Column(name = "streak_frozen_until")
    private OffsetDateTime streakFrozenUntil;

    @Column(name = "plan_template_id")
    private UUID planTemplateId;

    @Column(name = "template_code")
    private String templateCode;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "streak_recovery_used_count", nullable = false)
    private int streakRecoveryUsedCount = 0;
}
