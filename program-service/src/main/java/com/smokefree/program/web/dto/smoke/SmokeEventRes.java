package com.smokefree.program.web.dto.smoke;

import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SmokeEventRes(
        UUID id,
        UUID programId,
        SmokeEventType eventType,
        OffsetDateTime eventAt,
        String note,
        Instant createdAt
) {
    public static SmokeEventRes from(SmokeEvent e) {
        return new SmokeEventRes(
                e.getId(),
                e.getProgramId(),
                e.getEventType(),
                e.getEventAt(),
                e.getNote(),
                e.getCreatedAt()
        );
    }
}
