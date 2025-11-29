package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.Streak;
import com.smokefree.program.domain.model.StreakBreak;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.repo.StreakBreakRepository;
import com.smokefree.program.domain.repo.StreakRepository;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.streak.StreakBreakRes;
import com.smokefree.program.web.dto.streak.StreakView;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final StreakRepository streakRepo;
    private final StreakBreakRepository breakRepo;
    private final ProgramRepository programRepository;

    private static int daysBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return 0;
        var s = start.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        var e = end.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        return (int) ChronoUnit.DAYS.between(s, e);
    }

    @Override
    @Transactional
    public StreakView start(UUID programId, OffsetDateTime startedAt) {
        ensureProgramAccess(programId, false);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isPresent()) {
            var s = cur.get();
            int days = daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC));
            program.setStreakCurrent(days);
            programRepository.save(program);
            return new StreakView(s.getId(), days, program.getStreakBest(), daysWithoutSmoke(program), s.getStartedAt(), s.getEndedAt());
        }
        var s = new Streak();
        s.setId(UUID.randomUUID());
        s.setProgramId(programId);
        s.setStartedAt(startedAt != null ? startedAt : OffsetDateTime.now(ZoneOffset.UTC));
        streakRepo.save(s);

        program.setStreakCurrent(0);
        programRepository.save(program);

        return new StreakView(s.getId(), 0, program.getStreakBest(), daysWithoutSmoke(program), s.getStartedAt(), null);
    }

    @Override
    @Transactional
    public StreakView breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String note) {
        ensureProgramAccess(programId, false);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

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
            int length = Math.max(daysBetween(s.getStartedAt(), end), 0);
            s.setEndedAt(end);
            s.setLengthDays(length);
            streakRepo.save(s);
        }

        var b = new StreakBreak();
        b.setId(UUID.randomUUID()); // Ensure ID is set
        b.setStreakId(s.getId());
        b.setProgramId(programId);
        b.setSmokeEventId(smokeEventId);
        b.setBrokenAt(end);
        b.setPrevStreakDays(s.getLengthDays());
        b.setNote(note);
        breakRepo.save(b);

        program.setStreakCurrent(0);
        if (s.getLengthDays() != null && s.getLengthDays() > program.getStreakBest()) {
            program.setStreakBest(s.getLengthDays());
        }
        programRepository.save(program);

        return new StreakView(s.getId(), s.getLengthDays() == null ? 0 : s.getLengthDays(), program.getStreakBest(), daysWithoutSmoke(program), s.getStartedAt(), s.getEndedAt());
    }

    @Override
    public StreakView current(UUID programId) {
        ensureProgramAccess(programId, true);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        var cur = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        if (cur.isEmpty()) return new StreakView(null, 0, program.getStreakBest(), daysWithoutSmoke(program), null, null);
        
        var s = cur.get();
        int days = daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC));
        program.setStreakCurrent(days);
        if (days > program.getStreakBest()) {
            program.setStreakBest(days);
        }
        programRepository.save(program);

        return new StreakView(s.getId(), days, program.getStreakBest(), daysWithoutSmoke(program), s.getStartedAt(), s.getEndedAt());
    }

    @Override
    public List<StreakView> history(UUID programId, int size) {
        ensureProgramAccess(programId, true);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        return streakRepo.findByProgramIdOrderByStartedAtDesc(programId, PageRequest.of(0, Math.max(size, 1)))
                .stream()
                .map(s -> new StreakView(
                        s.getId(),
                        s.getEndedAt() == null
                                ? daysBetween(s.getStartedAt(), OffsetDateTime.now(ZoneOffset.UTC))
                                : (s.getLengthDays() == null ? 0 : s.getLengthDays()),
                        program.getStreakBest(),
                        daysWithoutSmoke(program),
                        s.getStartedAt(),
                        s.getEndedAt()))
                .toList();
    }

    @Override
    public List<StreakBreakRes> breaks(UUID programId, int size) {
        ensureProgramAccess(programId, true);
        return breakRepo.findByProgramIdOrderByBrokenAtDesc(programId, PageRequest.of(0, Math.max(size, 1)))
                .stream()
                .map(b -> new StreakBreakRes(
                        b.getId(), b.getProgramId(), b.getStreakId(), b.getSmokeEventId(),
                        b.getBrokenAt(), b.getPrevStreakDays(), b.getNote(), b.getCreatedAt()))
                .toList();
    }

    private void ensureProgramAccess(UUID programId, boolean allowCoachReadOnly) {
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
        if (isCoach && !allowCoachReadOnly) {
            throw new ForbiddenException("Coach cannot modify streak for program " + programId);
        }
    }

    private int daysWithoutSmoke(Program program) {
        try {
            if (program.getLastSmokeAt() != null) {
                return (int) Math.max(0, ChronoUnit.DAYS.between(program.getLastSmokeAt().toLocalDate(), java.time.LocalDate.now()));
            }
            if (program.getStartDate() != null) {
                return (int) Math.max(0, ChronoUnit.DAYS.between(program.getStartDate(), java.time.LocalDate.now()));
            }
        } catch (Exception ignore) {}
        return 0;
    }
}
