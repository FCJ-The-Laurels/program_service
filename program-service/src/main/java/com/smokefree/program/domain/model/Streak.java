package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "streaks", schema = "program")
@Getter @Setter
public class Streak {
    @Id private UUID id;

    @Column(name="program_id", nullable=false)
    private UUID programId;

    @Column(name="started_at", nullable=false)
    private OffsetDateTime startedAt;

    @Column(name="ended_at")
    private OffsetDateTime endedAt;

    @Column(name="length_days")
    private Integer lengthDays;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @PrePersist void pre() {
        if (createdAt == null) createdAt = Instant.now();
        if (startedAt == null) startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (id == null) id = UUID.randomUUID();
    }
}
