package com.smokefree.program.web.dto.me;

import java.time.Instant;
import java.util.UUID;

public record DueQuizRes(
        UUID templateId,
        String templateName,
        Instant dueAt,
        boolean isOverdue
) {}

