package com.smokefree.program.web.dto.step;

import java.time.OffsetDateTime;

public record RescheduleStepReq(
        OffsetDateTime newScheduledAt
) {}

