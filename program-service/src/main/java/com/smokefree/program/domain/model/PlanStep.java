package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "plan_steps", schema = "program",
        indexes = {
                @Index(name="idx_plan_steps_template_day_slot", columnList="template_id,day_no,slot")
        })
public class PlanStep {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "template_id", nullable = false, columnDefinition = "uuid")
    private UUID templateId;

    @Column(name = "day_no", nullable = false)
    private Integer dayNo;

    @Column(nullable = false)
    private LocalTime slot;

    @Column(nullable = false)
    private String title;

    @Column
    private String details;

    @Column(name = "max_minutes")
    private Integer maxMinutes;

    // nếu bạn có cột này trong DB (đã dùng ở service)
    @Column(name = "module_code")
    private String moduleCode;

    @Builder.Default
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // --- lifecycle: CHỈ 1 PrePersist ---
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    // KHÔNG thêm 1 PrePersist nào khác ở class này.
    // Nếu cần updatedAt thì dùng PreUpdate (và thêm field updatedAt):
    // @PreUpdate
    // void preUpdate() { this.updatedAt = OffsetDateTime.now(); }
}
