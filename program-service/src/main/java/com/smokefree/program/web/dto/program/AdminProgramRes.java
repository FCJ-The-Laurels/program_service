package com.smokefree.program.web.dto.program;

import com.smokefree.program.domain.model.ProgramStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO tóm tắt Program cho admin (phân trang).
 */
public record AdminProgramRes(
        UUID id,
        UUID userId,
        ProgramStatus status,
        Integer planDays,
        LocalDate startDate,
        Integer currentDay,
        String templateCode,
        String templateName,
        Instant createdAt
) {}
