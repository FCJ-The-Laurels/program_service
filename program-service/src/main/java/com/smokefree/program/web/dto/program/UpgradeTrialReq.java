package com.smokefree.program.web.dto.program;

public record UpgradeTrialReq(
        String paymentId,
        String paymentProofUrl
) {}

