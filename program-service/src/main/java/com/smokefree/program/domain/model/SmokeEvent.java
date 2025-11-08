package com.smokefree.program.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "smoke_events", schema = "program")
public class SmokeEvent {
    @Id private UUID id;

    @Column(name = "program_id", nullable = false) private UUID programId;
    @Column(name = "user_id",    nullable = false) private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false,
            columnDefinition = "program.smoke_event_type")
    private SmokeEventType eventType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kind", nullable = false,
            columnDefinition = "program.smoke_event_kind")
    private SmokeEventKind kind;

    @Column(name = "note") private String note;

    @Column(name = "event_at",    nullable = false) private OffsetDateTime eventAt;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Column(name = "created_at",  nullable = false) private Instant createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null)  createdAt  = Instant.now();
        if (eventAt == null)    eventAt    = OffsetDateTime.now(ZoneOffset.UTC);
        if (occurredAt == null) occurredAt = eventAt;
        if (kind == null && eventType == SmokeEventType.SMOKE) {
            kind = SmokeEventKind.SLIP;  // mặc định
        }
    }

}
