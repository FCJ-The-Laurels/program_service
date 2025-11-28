package com.smokefree.program.domain.service;

import com.smokefree.program.domain.service.SubscriptionService;
import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;
import com.smokefree.program.web.dto.subscription.UpgradeReq;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Override
    public SubscriptionStatusRes getStatus(UUID userId) {
        // TODO: Lấy từ user-service. Tạm mock BASIC
        return new SubscriptionStatusRes("BASIC", "ACTIVE", null);
    }

    @Override
    public SubscriptionStatusRes upgrade(UUID userId, UpgradeReq req) {
        // TODO: charge + call user-service. Tạm mock 30 ngày
        String target = req.targetTier().toUpperCase();
        if (!target.equals("PREMIUM") && !target.equals("VIP")) {
            throw new IllegalArgumentException("targetTier must be PREMIUM or VIP");
        }
        return new SubscriptionStatusRes(target, "ACTIVE", Instant.now().plus(30, ChronoUnit.DAYS));
    }
}
