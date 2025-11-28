package com.smokefree.program.web.dto.smoke;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SmokeEventRes(
        UUID id,
        OffsetDateTime occurredAt,
        String kind,
        Integer puffs,
        String reason,
        String eventType,
        OffsetDateTime eventAt,
        String note
) {
    public static SmokeEventRes from(com.smokefree.program.domain.model.SmokeEvent event) {
        return new SmokeEventRes(
                event.getId(),
                event.getOccurredAt(),
                event.getKind() != null ? event.getKind().name() : null,
                event.getPuffs(),
                event.getReason(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getEventAt(),
                event.getNote()
        );
    }
}

