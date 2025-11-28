package com.smokefree.program.web.dto.streak;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * StreakView - Thông tin hiện tại về streak của 1 program
 *
 * Fields:
 * - streakId: ID của streak hiện tại (hoặc null nếu chưa bắt đầu)
 * - currentStreak: Số ngày hiện tại (tính từ startedAt đến now)
 * - bestStreak: Số ngày tốt nhất (từ program entity)
 * - daysWithoutSmoke: Số ngày không hút (từ lastSmokeAt hoặc startDate)
 * - startedAt: Ngày bắt đầu streak hiện tại
 * - endedAt: Ngày kết thúc streak (null nếu chưa break)
 */
public record StreakView(
        UUID streakId,
        Integer currentStreak,
        Integer bestStreak,
        Integer daysWithoutSmoke,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt
) {
    // Constructor cho backward compatibility
    public StreakView(UUID streakId, int days, OffsetDateTime startedAt, OffsetDateTime endedAt) {
        this(streakId, days, null, null, startedAt, endedAt);
    }
}
