package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.ProgramStatus;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.service.quiz.QuizFlowService;
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.me.*;
import com.smokefree.program.web.dto.subscription.SubscriptionStatusRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeServiceImpl implements MeService {

    private final SubscriptionService subscriptionService;
    private final ProgramRepository programRepository;
    private final QuizFlowService quizFlowService;
    private final StreakService streakService;

    @Override
    @Transactional(readOnly = true)
    public DashboardRes dashboard(UUID userId) {
        log.info("[Dashboard] Called for userId: {}", userId);

        // 1. Lấy subscription status
        SubscriptionStatusRes subStatus = subscriptionService.getStatus(userId);
        SubscriptionRes subscription = new SubscriptionRes(
                subStatus.tier(),
                subStatus.status(),
                subStatus.expiresAt()
        );

        // 2. Lấy program active
        Program programEntity = programRepository
                .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
                .orElse(null);

        ActiveProgramRes activeProgram = null;
        List<DueQuizRes> dueQuizzes = List.of();
        StreakInfoRes streakInfo = null;

        if (programEntity != null) {
            if (programEntity.getTrialEndExpected() != null &&
                    Instant.now().isAfter(programEntity.getTrialEndExpected())) {
                throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
            }
            log.info("[Dashboard] Found active program: {}", programEntity.getId());

            // 3. Map ActiveProgramRes
            Integer trialRemainingDays = null;
            if (programEntity.getTrialStartedAt() != null &&
                    programEntity.getTrialEndExpected() != null) {
                long remaining = ChronoUnit.DAYS.between(
                        Instant.now(),
                        programEntity.getTrialEndExpected()
                );
                trialRemainingDays = (int) Math.max(0, remaining);
            }

            activeProgram = new ActiveProgramRes(
                    programEntity.getId(),
                    programEntity.getTemplateCode(),
                    programEntity.getTemplateName(),
                    programEntity.getStatus().name(),
                    programEntity.getCurrentDay(),
                    programEntity.getPlanDays(),
                    programEntity.getTrialStartedAt() != null,
                    trialRemainingDays,
                    programEntity.getCreatedAt()
            );

            // 4. Lấy quiz due
            try {
                // ✅ FIX: quizFlowService.listDue() trả về List<DueItem>, cần convert sang List<DueQuizRes>
                var dueItems = quizFlowService.listDue(userId);
                dueQuizzes = dueItems.stream()
                        .map(item -> new DueQuizRes(
                                item.templateId(),
                                item.templateName(),
                                item.dueAt(),
                                item.isOverdue()
                        ))
                        .toList();
                log.info("[Dashboard] Found {} due quizzes", dueQuizzes.size());
            } catch (Exception e) {
                log.warn("[Dashboard] Error getting due quizzes: {}", e.getMessage());
                dueQuizzes = List.of();
            }

            // 5. Lấy streak info
            try {
                var streak = streakService.current(programEntity.getId());
                streakInfo = new StreakInfoRes(
                        streak.currentStreak(),
                        programEntity.getStreakBest(),
                        calculateDaysWithoutSmoke(programEntity)
                );
                log.info("[Dashboard] Streak - current: {}, best: {}, days: {}",
                        streak.currentStreak(), programEntity.getStreakBest(), calculateDaysWithoutSmoke(programEntity));
            } catch (Exception e) {
                log.warn("[Dashboard] Error getting streak info: {}", e.getMessage());
                streakInfo = new StreakInfoRes(null, 0, 0);
            }
        } else {
            log.info("[Dashboard] No active program found for user: {}", userId);
            streakInfo = new StreakInfoRes(null, 0, 0);
        }

        return new DashboardRes(
                userId,
                subscription,
                activeProgram,
                dueQuizzes,
                streakInfo
        );
    }

    /**
     * Helper: Tính daysWithoutSmoke
     * - Nếu lastSmokeAt != null: tính từ lastSmokeAt
     * - Nếu lastSmokeAt == null: tính từ startDate
     */
    private int calculateDaysWithoutSmoke(Program program) {
        try {
            if (program.getLastSmokeAt() != null) {
                long days = ChronoUnit.DAYS.between(
                    program.getLastSmokeAt().toLocalDate(),
                    java.time.LocalDate.now()
                );
                return (int) Math.max(0, days);
            } else {
                long days = ChronoUnit.DAYS.between(
                    program.getStartDate(),
                    java.time.LocalDate.now()
                );
                return (int) Math.max(0, days);
            }
        } catch (Exception e) {
            log.warn("[Dashboard] Error calculating daysWithoutSmoke: {}", e.getMessage());
            return 0;
        }
    }

}
