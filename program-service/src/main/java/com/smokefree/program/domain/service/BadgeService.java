package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý việc kiểm tra và trao huy hiệu cho người dùng.
 * Hỗ trợ 3 loại huy hiệu: Program Milestone, Streak, Quiz Progress.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepo;
    private final UserBadgeRepository userBadgeRepo;
    private final QuizResultRepository quizResultRepo;

    // Định nghĩa các mã huy hiệu (Hardcoded để match với Seed Data)
    private static final String PROG_LV1 = "PROG_LV1";
    private static final String PROG_LV2 = "PROG_LV2";
    private static final String PROG_LV3 = "PROG_LV3";

    private static final String STREAK_LV1 = "STREAK_LV1";
    private static final String STREAK_LV2 = "STREAK_LV2";
    private static final String STREAK_LV3 = "STREAK_LV3";

    private static final String QUIZ_LV1 = "QUIZ_LV1";
    private static final String QUIZ_LV2 = "QUIZ_LV2";
    private static final String QUIZ_LV3 = "QUIZ_LV3";

    /**
     * Kiểm tra và trao huy hiệu Lộ trình (Program Milestone).
     * Điều kiện tiên quyết: Chương trình KHÔNG ĐƯỢC PAUSE (hasPaused == false).
     */
    @Async
    @Transactional
    public void checkProgramMilestone(Program program) {
        log.info("[Badge] Checking Program Milestone for user: {}", program.getUserId());

        if (program.isHasPaused()) {
            log.info("[Badge] Program has paused history. Skip milestone badges.");
            return;
        }

        int currentDay = program.getCurrentDay();
        int totalDays = program.getPlanDays();

        // Level 1: Khởi hành (Ngày 1)
        if (currentDay >= 1) {
            awardBadge(program.getUserId(), program.getId(), PROG_LV1);
        }

        // Level 2: Kiên trì (Nửa chặng đường)
        if (currentDay >= totalDays / 2) {
            awardBadge(program.getUserId(), program.getId(), PROG_LV2);
        }

        // Level 3: Về đích (Hoàn thành & Active -> Completed)
        if (program.getStatus() == ProgramStatus.COMPLETED) {
            awardBadge(program.getUserId(), program.getId(), PROG_LV3);
        }
    }

    /**
     * Kiểm tra và trao huy hiệu Chuỗi cai thuốc (Streak).
     */
    @Async
    @Transactional
    public void checkStreak(Program program, int daysSmokeFree) {
        log.info("[Badge] Checking Streak for user: {}, days: {}", program.getUserId(), daysSmokeFree);

        int totalDays = program.getPlanDays();

        // Level 1: 7 ngày
        if (daysSmokeFree >= 7) {
            awardBadge(program.getUserId(), program.getId(), STREAK_LV1);
        }

        // Level 2: Nửa lộ trình
        if (daysSmokeFree >= totalDays / 2) {
            awardBadge(program.getUserId(), program.getId(), STREAK_LV2);
        }

        // Level 3: Full lộ trình
        if (daysSmokeFree >= totalDays) {
            awardBadge(program.getUserId(), program.getId(), STREAK_LV3);
        }
    }

    /**
     * Kiểm tra và trao huy hiệu Tiến triển Quiz.
     * Trigger sau khi submit một quiz.
     */
    @Async
    @Transactional
    public void checkQuizProgress(Program program) {
        log.info("[Badge] Checking Quiz Progress for user: {}", program.getUserId());

        // Lấy các kết quả gần nhất
        List<QuizResult> results = quizResultRepo.findRecentByProgramId(program.getId(), 100);
        if (results.isEmpty()) return;

        // Level 1: Hoàn thành quiz đầu tiên
        if (results.size() >= 1) {
            awardBadge(program.getUserId(), program.getId(), QUIZ_LV1);
        }

        // Level 2: Tiến triển tốt (2 lần gần nhất severity không tăng)
        // Note: results order by createdAt DESC -> [0] là mới nhất, [1] là cũ hơn
        if (results.size() >= 2) {
            QuizResult current = results.get(0);
            QuizResult prev = results.get(1);
            
            // So sánh ordinal của Enum Severity (LOW < MODERATE < HIGH < SEVERE)
            // Nếu current <= prev -> Tốt/Giữ vững
            if (current.getSeverity().ordinal() <= prev.getSeverity().ordinal()) {
                awardBadge(program.getUserId(), program.getId(), QUIZ_LV2);
            }
        }

        // Level 3: Làm chủ (Tất cả quiz đều LOW hoặc quiz cuối cùng LOW và đã hoàn thành program)
        // Logic đơn giản cho MVP: Nếu quiz cuối cùng là LOW và program sắp hết
        QuizResult latest = results.get(0);
        if (program.getCurrentDay() >= program.getPlanDays() - 7 && latest.getSeverity() == SeverityLevel.LOW) {
             awardBadge(program.getUserId(), program.getId(), QUIZ_LV3);
        }
    }

    private void awardBadge(UUID userId, UUID programId, String badgeCode) {
        Badge badge = badgeRepo.findByCode(badgeCode).orElse(null);
        if (badge == null) {
            log.warn("[Badge] Badge code not found: {}", badgeCode);
            return;
        }

        if (userBadgeRepo.existsByUserIdAndBadgeIdAndProgramId(userId, badge.getId(), programId)) {
            // Đã nhận rồi -> bỏ qua
            return;
        }

        UserBadge userBadge = UserBadge.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .badgeId(badge.getId())
                .programId(programId)
                .build(); // earnedAt tự động set bởi @PrePersist

        userBadgeRepo.save(userBadge);
        log.info("[Badge] AWARDED! User: {}, Badge: {}", userId, badgeCode);
    }
}
