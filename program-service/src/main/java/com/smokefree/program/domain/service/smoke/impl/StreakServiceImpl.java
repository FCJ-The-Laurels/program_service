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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakServiceImpl implements StreakService {

    private final StreakRepository streakRepo;
    private final StreakBreakRepository breakRepo;
    private final ProgramRepository programRepository;

    private static int daysBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return 0;
        var s = start.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        var e = end.toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
        // Sửa lỗi: cộng thêm 1 để tính cả ngày bắt đầu
        return (int) ChronoUnit.DAYS.between(s, e) + 1;
    }

    @Override
    @Transactional
    public void startOrContinueStreak(UUID programId) {
        // This method is called when a day is completed.
        // We now rely on the `programs.streak_current` column, which is updated by StepAssignmentService.
        // However, we can ensure a historical streak record exists.
        ensureProgramAccess(programId, false);
        Optional<Streak> currentStreak = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);

        if (currentStreak.isEmpty()) {
            log.info("[Streak] No active historical streak found for program {}. Starting a new one.", programId);
            start(programId, OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            log.info("[Streak] Program {} already has an active historical streak. No action needed.", programId);
        }
    }

    @Override
    @Transactional
    public StreakView start(UUID programId, OffsetDateTime startedAt) {
        ensureProgramAccess(programId, false);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
        
        // Only create if no active historical streak exists
        streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId).ifPresent(existing -> {
            throw new IllegalStateException("An active historical streak already exists for this program.");
        });

        Streak s = new Streak();
        s.setId(UUID.randomUUID());
        s.setProgramId(programId);
        s.setStartedAt(startedAt != null ? startedAt : OffsetDateTime.now(ZoneOffset.UTC));
        streakRepo.save(s);

        // Reset the counter in Program cache
        program.setStreakCurrent(0);
        programRepository.save(program);

        return new StreakView(s.getId(), 0, program.getStreakBest(), daysWithoutSmoke(program), s.getStartedAt(), null);
    }

    @Override
    @Transactional
    public StreakView breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String note) {
        // SỬA LỖI: Truyền `null` cho tham số `reason` còn thiếu
        StreakBreak breakRecord = breakStreakAndLog(programId, brokenAt, smokeEventId, null, note);
        Program program = programRepository.findById(programId).orElseThrow(() -> new NotFoundException("Program not found"));
        return new StreakView(breakRecord.getStreakId(), breakRecord.getPrevStreakDays(), program.getStreakBest(), daysWithoutSmoke(program), null, breakRecord.getBrokenAt());
    }


    @Override
    @Transactional
    public StreakBreak breakStreakAndLog(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String reason, String note) {
        ensureProgramAccess(programId, false);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        Streak streakToBreak = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId)
                .orElseThrow(() -> new NotFoundException("No active streak to break for program " + programId));

        OffsetDateTime end = brokenAt != null ? brokenAt : OffsetDateTime.now(ZoneOffset.UTC);
        int length = Math.max(1, daysBetween(streakToBreak.getStartedAt(), end));
        streakToBreak.setEndedAt(end);
        streakToBreak.setLengthDays(length);
        streakRepo.save(streakToBreak);

        StreakBreak b = new StreakBreak();
        b.setId(UUID.randomUUID());
        b.setStreakId(streakToBreak.getId());
        b.setProgramId(programId);
        b.setSmokeEventId(smokeEventId);
        b.setBrokenAt(end);
        b.setPrevStreakDays(length);
        b.setReason(reason); // <-- LƯU LẠI REASON
        b.setNote(note);
        breakRepo.save(b);

        program.setStreakCurrent(0);
        if (length > program.getStreakBest()) {
            program.setStreakBest(length);
        }
        programRepository.save(program);
        return b;
    }

    @Override
    @Transactional
    public void restoreStreak(UUID streakBreakId) {
        StreakBreak breakRecord = breakRepo.findById(streakBreakId)
                .orElseThrow(() -> new NotFoundException("StreakBreak record not found: " + streakBreakId));
        ensureProgramAccess(breakRecord.getProgramId(), false);
        
        Streak brokenStreak = streakRepo.findById(breakRecord.getStreakId())
                .orElseThrow(() -> new NotFoundException("The broken streak to restore was not found: " + breakRecord.getStreakId()));
        
        // "Revive" the historical streak record
        brokenStreak.setEndedAt(null);
        brokenStreak.setLengthDays(null);
        streakRepo.save(brokenStreak);

        // Update the cache in Program
        Program program = programRepository.findById(breakRecord.getProgramId()).orElseThrow(() -> new NotFoundException("Program not found"));
        // Sử dụng lại giá trị đã được tính toán chính xác từ breakRecord
        int restoredDays = breakRecord.getPrevStreakDays();
        program.setStreakCurrent(restoredDays);
        programRepository.save(program);

        log.info("Streak {} has been restored for program {}. Restored days: {}", brokenStreak.getId(), program.getId(), restoredDays);
    }

    /**
     * Lấy thông tin streak hiện tại.
     * PHIÊN BẢN MỚI: Chỉ đọc dữ liệu từ bảng `programs` làm nguồn chân lý chính.
     */
    @Override
    @Transactional(readOnly = true)
    public StreakView current(UUID programId) {
        ensureProgramAccess(programId, true);
        
        // 1. Lấy Program làm nguồn chân lý cho các giá trị hiện tại và tốt nhất.
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        // 2. (Tùy chọn) Tìm bản ghi streak lịch sử đang hoạt động để lấy ngày bắt đầu.
        Optional<Streak> currentStreakOpt = streakRepo.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(programId);
        
        UUID streakId = currentStreakOpt.map(Streak::getId).orElse(null);
        OffsetDateTime startedAt = currentStreakOpt.map(Streak::getStartedAt).orElse(null);

        // 3. Trả về DTO, luôn tin tưởng vào giá trị trong bảng `programs`.
        return new StreakView(
            streakId,
            program.getStreakCurrent(),
            program.getStreakBest(),
            daysWithoutSmoke(program),
            startedAt,
            null // Nếu đang current thì endedAt luôn là null
        );
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

    private void ensureProgramAccess(UUID programId, boolean allowCoachWrite) {
        if (SecurityUtil.hasRole("ADMIN")) {
            return;
        }
        UUID userId = SecurityUtil.requireUserId();
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        if (program.getTrialEndExpected() != null && java.time.Instant.now().isAfter(program.getTrialEndExpected())) {
            throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        boolean isOwner = program.getUserId().equals(userId);
        boolean isCoach = program.getCoachId() != null && program.getCoachId().equals(userId) && SecurityUtil.hasRole("COACH");
        if (!isOwner && !isCoach) {
            throw new ForbiddenException("Access denied for program " + programId);
        }
        if (isCoach && !allowCoachWrite) {
            throw new ForbiddenException("Coach cannot modify steps for program " + programId);
        }
    }

    /**
     * Calculates the number of days without smoke.
     * <p>
     * CRITICAL: Uses {@code ZoneOffset.UTC} for "now" to ensure consistency with the database
     * (where dates are stored in UTC). Using system default timezone would lead to
     * inconsistent results across servers in different regions.
     * </p>
     */
    private int daysWithoutSmoke(Program program) {
        try {
            java.time.LocalDate nowUtc = java.time.LocalDate.now(ZoneOffset.UTC);
            if (program.getLastSmokeAt() != null) {
                // Convert OffsetDateTime to LocalDate at UTC
                java.time.LocalDate smokeDate = program.getLastSmokeAt().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
                return (int) Math.max(0, ChronoUnit.DAYS.between(smokeDate, nowUtc));
            }
            if (program.getStartDate() != null) {
                return (int) Math.max(0, ChronoUnit.DAYS.between(program.getStartDate(), nowUtc));
            }
        } catch (Exception ignore) {}
        return 0;
    }
}
