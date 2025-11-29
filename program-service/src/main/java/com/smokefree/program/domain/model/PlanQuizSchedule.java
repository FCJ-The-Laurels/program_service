package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "plan_quiz_schedules", schema = "program")
public class PlanQuizSchedule {

    @Id
    private UUID id;

    @Column(name = "plan_template_id", nullable = false)
    private UUID planTemplateId;

    @Column(name = "quiz_template_id", nullable = false)
    private UUID quizTemplateId;

    @Column(name = "start_offset_day", nullable = false)
    private Integer startOffsetDay = 1; // Ngày trong lộ trình (1-based)

    @Column(name = "every_days", nullable = false)
    private Integer everyDays = 0; // 0 = chỉ một lần, >0 = lặp lại

    @Column(name = "order_no")
    private Integer orderNo; // Dùng để cố định thứ tự hiển thị/ưu tiên

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (startOffsetDay == null) startOffsetDay = 1;
        if (everyDays == null) everyDays = 0;
    }
}
