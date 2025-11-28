package com.smokefree.program.web.dto.subscription;

import java.time.Instant;

/**
 * SubscriptionStatusRes - Thông tin subscription của user
 *
 * Fields:
 * - tier: BASIC|PREMIUM|VIP
 * - status: ACTIVE|TRIALING|GRACE|EXPIRED
 * - expiresAt: Thời gian hết hạn (null nếu lifetime/BASIC)
 */
public record SubscriptionStatusRes(
        String tier,
        String status,
        Instant expiresAt
) {}

