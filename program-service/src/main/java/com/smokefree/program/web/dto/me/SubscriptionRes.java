package com.smokefree.program.web.dto.me;

import java.time.Instant;

public record SubscriptionRes(
        String tier,
        String status,
        Instant expiresAt
) {}

