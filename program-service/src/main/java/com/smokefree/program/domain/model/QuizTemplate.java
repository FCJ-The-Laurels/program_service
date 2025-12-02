package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "quiz_templates",
        schema = "program",
        indexes = {
                @Index(name = "idx_quiz_template_scope_owner", columnList = "scope, owner_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quiz_template_name_scope_owner_version",
                columnNames = {"name","scope","owner_id","version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizTemplate {

    @Id
    private UUID id;

    @Column(name = "code", unique = true) // Thêm cột code, đảm bảo là duy nhất
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    // DB: DEFAULT 1 NOT NULL
    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizTemplateStatus status;

    @Column(name = "language_code")
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizTemplateScope scope;     // SYSTEM | COACH (MVP luôn SYSTEM)

    @Column(name = "owner_id")
    private UUID ownerId;                // luôn null trong MVP (template hệ thống)

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.questionNo ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        // version mặc định 1
        if (version == null) {
            version = 1;
        }
        // trạng thái mặc định DRAFT
        if (status == null) {
            status = QuizTemplateStatus.DRAFT;
        }
        // ngôn ngữ mặc định 'vi'
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "vi";
        }

        // MVP: mọi template là SYSTEM, ownerId = null
        scope   = QuizTemplateScope.SYSTEM;
        ownerId = null;

        // name bắt buộc
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("QuizTemplate.name must not be empty");
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();

        // Giữ ràng buộc SYSTEM-only trong mọi lần update
        scope   = QuizTemplateScope.SYSTEM;
        ownerId = null;
    }
}
