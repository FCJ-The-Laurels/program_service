package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.SmokeEventRepository;
import com.smokefree.program.domain.repo.StreakRecoveryConfigRepository;
import com.smokefree.program.domain.service.QuizAssignmentService;
import com.smokefree.program.domain.service.smoke.SmokeEventService;
// import com.smokefree.program.domain.service.smoke.StepAssignmentService; // Tạm thời không dùng
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmokeEventServiceImpl implements SmokeEventService {

    private final SmokeEventRepository smokeEventRepository;
    private final ProgramRepository programRepository;
    private final StreakService streakService;
    private final StreakRecoveryConfigRepository recoveryConfigRepo;
    private final QuizAssignmentService quizAssignmentService;
    // private final StepAssignmentService stepAssignmentService;

    @Override
    @Transactional
    public SmokeEvent create(UUID programId, CreateSmokeEventReq req) {
        ensureProgramAccess(programId, false);
        log.info("[SmokeEvent] create for programId: {}, kind: {}", programId, req.kind());

        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
        log.debug("[SmokeEvent] Program loaded: id={}, currentStreak={}, bestStreak={}, recoveryUsedCount={}", 
                  program.getId(), program.getStreakCurrent(), program.getStreakBest(), program.getStreakRecoveryUsedCount());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        SmokeEvent event = SmokeEvent.builder()
            .programId(programId)
            .userId(program.getUserId())
            .kind(req.kind())
            .eventType(req.eventType())
            .occurredAt(req.occurredAt() != null ? req.occurredAt() : now)
            .eventAt(req.eventAt() != null ? req.eventAt() : now)
            .note(req.note())
            .puffs(req.puffs())
            .reason(req.reason())
            .build();
        log.debug("[SmokeEvent] New SmokeEvent built: kind={}", event.getKind());

        smokeEventRepository.save(event);
        log.debug("[SmokeEvent] SmokeEvent saved (first time): id={}", event.getId());

        program.setLastSmokeAt(now);
        log.debug("[SmokeEvent] Program.lastSmokeAt updated to {}", now);

        if (req.kind() == SmokeEventKind.SLIP || req.kind() == SmokeEventKind.RELAPSE) {
            log.info("[SmokeEvent] Breaking streak for programId: {}", programId);
            var breakRecord = streakService.breakStreakAndLog(programId, now, event.getId(), req.note()); 
            log.debug("[SmokeEvent] Streak break has been logged.");

            boolean recoveryAssigned = handleRecoveryAssignment(program, breakRecord.getId());
            
            // Nếu không còn cơ hội phục hồi (hết lượt), thực hiện "hard reset"
            if (!recoveryAssigned) {
                log.warn("[SmokeEvent] No recovery was assigned. Performing hard reset by starting a new streak.");
                streakService.start(programId, now);
            }

        } else {
            log.info("[SmokeEvent] Continuing/starting streak for programId: {}", programId);
            streakService.startOrContinueStreak(programId); // Sử dụng phương thức này để đảm bảo có streak
            int currentStreak = program.getStreakCurrent() + 1;
            program.setStreakCurrent(currentStreak);
            log.debug("[SmokeEvent] Program.streakCurrent incremented to {}", currentStreak);

            if (currentStreak > program.getStreakBest()) {
                program.setStreakBest(currentStreak);
                log.debug("[SmokeEvent] Program.streakBest updated to {}", currentStreak);
            }
        }

        programRepository.save(program);
        log.debug("[SmokeEvent] Program saved: id={}, currentStreak={}, bestStreak={}, recoveryUsedCount={}", 
                  program.getId(), program.getStreakCurrent(), program.getStreakBest(), program.getStreakRecoveryUsedCount());

        return event;
    }

    /**
     * Gán nhiệm vụ phục hồi và trả về true nếu thành công, false nếu hết lượt.
     */
    private boolean handleRecoveryAssignment(Program program, UUID streakBreakId) {
        int nextAttemptOrder = program.getStreakRecoveryUsedCount() + 1;
        log.info("[Recovery] Attempting to find recovery config for attempt #{}", nextAttemptOrder);

        Optional<com.smokefree.program.domain.model.StreakRecoveryConfig> configOpt = recoveryConfigRepo.findByAttemptOrder(nextAttemptOrder);

        if (configOpt.isEmpty()) {
            log.warn("[Recovery] No recovery config found for attempt #{}. No recovery task will be assigned.", nextAttemptOrder);
            return false; // Hết cơ hội
        }

        var config = configOpt.get();
        log.info("[Recovery] Found recovery config: attemptOrder={}, moduleCode={}", config.getAttemptOrder(), config.getModuleCode());

        String moduleCode = config.getModuleCode();

        log.info("[Recovery] Assigning recovery quiz with module code: {}", moduleCode);
        quizAssignmentService.assignRecoveryQuiz(program.getId(), moduleCode, streakBreakId);

        // KHÔNG ĐẾM TĂNG Ở ĐÂY. Bộ đếm sẽ chỉ được tăng sau khi quiz được nộp thành công.
        // program.setStreakRecoveryUsedCount(nextAttemptOrder);
        // log.info("[Recovery] Incremented streak_recovery_used_count to {}", nextAttemptOrder);
        return true; // Gán thành công
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

        // Chặn khi hết trial
        if (program.getTrialEndExpected() != null && java.time.Instant.now().isAfter(program.getTrialEndExpected())) {
            throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

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
