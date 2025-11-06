// src/main/java/com/smokefree/program/domain/model/ContentModule.java
package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "content_modules", schema = "program",
        uniqueConstraints = @UniqueConstraint(columnNames = {"code","lang","version"}))
public class ContentModule {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String code;          // ví dụ: "EDU_BENEFITS_24H"

    @Column(nullable = false)
    private String type;          // ví dụ: "EDU_SLIDES_QUIZ"

    @Builder.Default
    @Column(nullable = false)
    private String lang = "vi";   // "vi" | "en"

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    // dùng jsonb (PostgreSQL)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    // --- lifecycle ---
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
