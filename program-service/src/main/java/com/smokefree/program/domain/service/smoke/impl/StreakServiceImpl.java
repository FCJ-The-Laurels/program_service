package com.smokefree.program.domain.service.smoke.impl;


import com.smokefree.program.domain.model.Streak;
import com.smokefree.program.domain.model.StreakBreak;
import com.smokefree.program.domain.repo.StreakBreakRepository;
import com.smokefree.program.domain.repo.StreakRepository;

import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.streak.StreakBreakRes;
import com.smokefree.program.web.dto.streak.StreakView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final StreakRepository streakRepo;
    private final StreakBreakRepository breakRepo;

    private static int daysBetween(OffsetDateTime start, OffsetDateTime end) {
        var s = start.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        var e = end.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        return (int) ChronoUnit.DAYS.between(s, e);
    }

    @Override @Transactional
    public StreakView start(UUID programId, OffsetDateTime startedAt) {
        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isPresent()) {
            var s = cur.get();
            int days = daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC));
            return new StreakView(s.getId(), days, s.getStartedAt(), s.getEndedAt());
        }
        var s = new Streak();
        s.setId(UUID.randomUUID());
        s.setProgramId(programId);
        s.setStartedAt(startedAt != null ? startedAt : OffsetDateTime.now(ZoneOffset.UTC));
        streakRepo.save(s);
        return new StreakView(s.getId(), 0, s.getStartedAt(), null);
    }

    @Override @Transactional
    public StreakView breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String note) {
        var s = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId)
                .orElseGet(() -> {
                    var ns = new Streak();
                    ns.setId(UUID.randomUUID());
                    ns.setProgramId(programId);
                    ns.setStartedAt(brokenAt != null ? brokenAt : OffsetDateTime.now(ZoneOffset.UTC));
                    return streakRepo.save(ns);
                });

        var end = brokenAt != null ? brokenAt : OffsetDateTime.now(ZoneOffset.UTC);
        if (s.getEndedAt() == null) {
            s.setEndedAt(end);
            s.setLengthDays(Math.max(daysBetween(s.getStartedAt(), end), 0));
            streakRepo.save(s);
        }

        var b = new StreakBreak();
        b.setStreakId(s.getId());
        b.setProgramId(programId);
        b.setSmokeEventId(smokeEventId);
        b.setBrokenAt(end);
        b.setPrevStreakDays(s.getLengthDays());
        b.setNote(note);
        breakRepo.save(b);

        // trả về tóm tắt streak vừa đóng
        return new StreakView(s.getId(), s.getLengthDays() == null ? 0 : s.getLengthDays(), s.getStartedAt(), s.getEndedAt());
    }

    @Override
    public StreakView current(UUID programId) {
        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isEmpty()) return new StreakView(null, 0, null, null);
        var s = cur.get();
        int days = daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC));
        return new StreakView(s.getId(), days, s.getStartedAt(), s.getEndedAt());
    }

    @Override
    public List<StreakView> history(UUID programId, int size) {
        return streakRepo.findByProgramIdOrderByStartedAtDesc(programId, PageRequest.of(0, Math.max(size, 1)))
                .stream()
                .map(s -> new StreakView(
                        s.getId(),
                        s.getEndedAt() == null
                                ? daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC))
                                : (s.getLengthDays() == null ? 0 : s.getLengthDays()),
                        s.getStartedAt(),
                        s.getEndedAt()))
                .toList();
    }

    @Override
    public List<StreakBreakRes> breaks(UUID programId, int size) {
        return breakRepo.findByProgramIdOrderByBrokenAtDesc(programId, PageRequest.of(0, Math.max(size, 1)))
                .stream()
                .map(b -> new StreakBreakRes(
                        b.getId(), b.getProgramId(), b.getStreakId(), b.getSmokeEventId(),
                        b.getBrokenAt(), b.getPrevStreakDays(), b.getNote(), b.getCreatedAt()))
                .toList();
    }
}
