package com.smokefree.program.domain.service.smoke.impl;


import com.smokefree.program.domain.model.Streak;
import com.smokefree.program.domain.model.StreakBreak;
import com.smokefree.program.domain.repo.StreakBreakRepository;
import com.smokefree.program.domain.repo.StreakRepository;

import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.streak.StreakView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final StreakRepository streakRepo;
    private final StreakBreakRepository breakRepo;

    @Override @Transactional
    public StreakView start(UUID programId, OffsetDateTime startedAt) {
        // nếu đã có streak đang mở thì trả về luôn
        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isPresent()) {
            var s = cur.get();
            int days = (int) Duration.between(s.getStartedAt().toInstant(), Instant.now()).toDays();
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
    public void breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String note) {
        var s = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId)
                .orElseGet(() -> {
                    var ns = new Streak();
                    ns.setId(UUID.randomUUID());
                    ns.setProgramId(programId);
                    ns.setStartedAt(brokenAt);
                    return streakRepo.save(ns);
                });

        if (s.getEndedAt() == null) {
            s.setEndedAt(brokenAt);
            int days = (int) Duration.between(s.getStartedAt().toInstant(), brokenAt.toInstant()).toDays();
            s.setLengthDays(Math.max(days, 0));
            streakRepo.save(s);
        }

        var b = new StreakBreak();
        b.setStreakId(s.getId());          // <-- quan trọng
        b.setProgramId(programId);
        b.setSmokeEventId(smokeEventId);
        b.setBrokenAt(brokenAt);            // <-- dùng property mới
        b.setPrevStreakDays(s.getLengthDays());
        b.setNote(note);
        breakRepo.save(b);
    }



    @Override
    public StreakView current(UUID programId) {
        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isEmpty()) return new StreakView(null, 0, null, null);
        var s = cur.get();
        int days = (int) Duration.between(s.getStartedAt().toInstant(), Instant.now()).toDays();
        return new StreakView(s.getId(), days, s.getStartedAt(), s.getEndedAt());
    }
}
