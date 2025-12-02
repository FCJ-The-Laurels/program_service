package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "streak_breaks", schema = "program")
@Getter @Setter
public class StreakBreak {
    @Id // Đã xóa @GeneratedValue
    private UUID id;

    @Column(name = "streak_id", nullable = false)
    private UUID streakId;                 // <-- thêm để khớp repo

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "smoke_event_id", nullable = false)
    private UUID smokeEventId;

    @Column(name = "broke_at", nullable = false)
    private OffsetDateTime brokenAt;       // <-- đổi tên property; map tới cột broke_at

    @Column(name = "prev_streak_days", nullable = false)
    private Integer prevStreakDays;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void pre() { 
        if (createdAt == null) createdAt = Instant.now(); 
        if (id == null) id = UUID.randomUUID(); // Đảm bảo ID được gán nếu chưa có
    }
}
