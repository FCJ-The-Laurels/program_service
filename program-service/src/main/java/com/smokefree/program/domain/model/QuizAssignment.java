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
@Table(name="quiz_assignments", schema="program")
@Getter @Setter
public class QuizAssignment {
    @Id private UUID id;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", nullable = false, columnDefinition = "program.assignment_scope")
    private AssignmentScope scope = AssignmentScope.DAY;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name="origin", columnDefinition="program.quiz_assignment_origin", nullable=false)
    private QuizAssignmentOrigin origin;
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
    @Column(name="template_id") private UUID templateId;
    @Column(name="program_id")  private UUID programId;
    @Column(name="every_days")  private Integer everyDays;  // KHỚP tên cột
    @Column(name="created_at", updatable=false) private Instant createdAt;
    @Column(name="created_by") private UUID createdBy;     // KHỚP tên cột
}

