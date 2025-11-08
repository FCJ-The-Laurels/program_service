package com.smokefree.program.domain.service.smoke.impl;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.model.SmokeEventType;
import com.smokefree.program.domain.repo.SmokeEventRepository;

import com.smokefree.program.domain.service.smoke.SmokeEventService;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmokeEventServiceImpl implements SmokeEventService {

    private final SmokeEventRepository smokeRepo;
    private final StreakService streakService;

    @Transactional
    public SmokeEvent create(UUID programId, CreateSmokeEventReq req) {
        UUID current = SecurityUtil.currentUserId();
        var when = req.eventAt() != null ? req.eventAt() : OffsetDateTime.now(ZoneOffset.UTC);

        SmokeEvent e = new SmokeEvent();
        e.setId(UUID.randomUUID());
        e.setProgramId(programId);
        e.setUserId(current);
        e.setEventType(req.eventType());
        e.setKind(req.kind() != null ? req.kind()
                : (req.eventType() == SmokeEventType.SMOKE ? SmokeEventKind.SLIP : null));
        e.setEventAt(when);
        e.setOccurredAt(when);
        e.setNote(req.note());
        e.setCreatedAt(Instant.now());      // <- tự set

        SmokeEvent saved = smokeRepo.saveAndFlush(e); // hoặc save(e)
        if (saved.getEventType() == SmokeEventType.SMOKE) {
            streakService.breakStreak(programId, when, saved.getId(), saved.getNote());
        }
        return saved;
    }
}

