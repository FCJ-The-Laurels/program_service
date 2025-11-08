package com.smokefree.program.web.dto.streak;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BreakStreakReq(
        OffsetDateTime brokenAt, // optional; null -> dùng now(UTC)
        UUID smokeEventId,       // optional; nếu do SmokeEvent gây ra thì điền
        String note              // optional
) {}
