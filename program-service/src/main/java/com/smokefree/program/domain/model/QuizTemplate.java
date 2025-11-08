package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
@Getter @Setter
public class QuizTemplate {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    // Cho phép null (để unique still work theo chuẩn Postgres – null != null)
    @Column(name = "version")
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuizTemplateStatus status; // DRAFT/PUBLISHED/ARCHIVED

    @Column(name = "language_code")
    private String languageCode;

    // NEW -----
    @Column(name = "scope", length = 20, nullable = false)
    private String scope;   // "system" | "coach"

    @Column(name = "owner_id")
    private UUID ownerId;   // null với system, có giá trị với coach
    // ---------

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Lưu ý: chỉ dùng mappedBy="template" nếu QuizTemplateQuestion có field @ManyToOne QuizTemplate template;
    // nếu child chỉ có templateId (không có back-ref entity), hãy đổi sang @OneToMany + @JoinColumn("template_id")
    // ở parent (unidirectional).
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.questionNo ASC")
    private List<QuizTemplateQuestion> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;

        // mặc định an toàn
        if (status == null) status = QuizTemplateStatus.DRAFT;

        // chuẩn hoá scope
        if (scope == null || scope.isBlank()) {
            scope = "system";
        } else {
            scope = scope.trim().toLowerCase(); // chỉ để "system" | "coach"
        }

        // name bắt buộc
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("QuizTemplate.name must not be empty");
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (scope != null) scope = scope.trim().toLowerCase();
    }
}
