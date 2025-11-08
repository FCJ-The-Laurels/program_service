package com.smokefree.program.web.dto.smoke;

import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.model.SmokeEventType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CreateSmokeEventReq(
        @NotNull SmokeEventType eventType, // SMOKE/URGE...
        String note,
        OffsetDateTime eventAt,
        OffsetDateTime occurredAt,
        @NotNull SmokeEventKind kind
        ) {}
