package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.SmokeEventRepository;
import com.smokefree.program.domain.service.smoke.SmokeEventService;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import com.smokefree.program.web.dto.smoke.SmokeEventStatisticsRes;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmokeEventServiceImpl implements SmokeEventService {

    private final SmokeEventRepository smokeEventRepository;
    private final ProgramRepository programRepository;
    private final StreakService streakService;

    @Override
    @Transactional
    public SmokeEvent create(UUID programId, CreateSmokeEventReq req) {
        ensureProgramAccess(programId, false);
        log.info("[SmokeEvent] create for programId: {}, kind: {}", programId, req.kind());

        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        SmokeEvent event = SmokeEvent.builder()
            .id(UUID.randomUUID())
            .programId(programId)
            .userId(program.getUserId())
            .kind(req.kind())
            .eventType(req.eventType())
            .occurredAt(req.occurredAt() != null ? req.occurredAt() : now)
            .eventAt(req.eventAt() != null ? req.eventAt() : now)
            .note(req.note())
            .build();

        program.setLastSmokeAt(now);

        if (req.kind() == SmokeEventKind.SLIP || req.kind() == SmokeEventKind.RELAPSE) {
            log.info("[SmokeEvent] Breaking streak for programId: {}", programId);
            streakService.breakStreak(programId, now, event.getId(), req.note());
            program.setStreakCurrent(0);
        } else {
            log.info("[SmokeEvent] Continuing/starting streak for programId: {}", programId);
            streakService.start(programId, now);
            int currentStreak = program.getStreakCurrent() + 1;
            program.setStreakCurrent(currentStreak);

            if (currentStreak > program.getStreakBest()) {
                program.setStreakBest(currentStreak);
            }
        }

        smokeEventRepository.save(event);
        programRepository.save(program);

        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmokeEvent> getHistory(UUID programId, int size) {
        ensureProgramAccess(programId, true);
        return smokeEventRepository.findByProgramIdOrderByEventAtDesc(programId).stream()
            .limit(Math.max(1, size))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SmokeEventStatisticsRes getStatistics(UUID programId, String period) {
        ensureProgramAccess(programId, true);
        List<SmokeEvent> allEvents = smokeEventRepository.findByProgramIdOrderByEventAtDesc(programId);

        LocalDate cutoffDate = LocalDate.now();
        if ("WEEK".equals(period)) {
            cutoffDate = cutoffDate.minusWeeks(1);
        } else if ("MONTH".equals(period)) {
            cutoffDate = cutoffDate.minusMonths(1);
        }

        LocalDate finalCutoffDate = cutoffDate;
        List<SmokeEvent> filteredEvents = allEvents.stream()
            .filter(e -> e.getOccurredAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate().isAfter(finalCutoffDate))
            .toList();

        int totalCount = filteredEvents.size();
        double avgPerDay = totalCount > 0 ? (double) totalCount / Math.max(1, ChronoUnit.DAYS.between(cutoffDate, LocalDate.now())) : 0.0;

        return new SmokeEventStatisticsRes(
            totalCount,
            filteredEvents.size(), 
            avgPerDay,
            List.of()
        );
    }

    private void ensureProgramAccess(UUID programId, boolean allowCoachWrite) {
        if (SecurityUtil.hasRole("ADMIN")) {
            return;
        }
        UUID userId = SecurityUtil.requireUserId();
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
        boolean isOwner = program.getUserId().equals(userId);
        boolean isCoach = program.getCoachId() != null && program.getCoachId().equals(userId) && SecurityUtil.hasRole("COACH");
        if (!isOwner && !isCoach) {
            throw new ForbiddenException("Access denied for program " + programId);
        }
        if (isCoach && !allowCoachWrite) {
            throw new ForbiddenException("Coach cannot modify smoke events for program " + programId);
        }
    }
}
