package com.smokefree.program.domain.service.smoke;

import com.smokefree.program.domain.model.StreakBreak;
import com.smokefree.program.web.dto.streak.StreakBreakRes;
import com.smokefree.program.web.dto.streak.StreakView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StreakService {
    StreakView start(UUID programId, OffsetDateTime startedAt);
    StreakView breakStreak(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String note);
    StreakView current(UUID programId);
    List<StreakView> history(UUID programId, int size);
    List<StreakBreakRes> breaks(UUID programId, int size);
    void startOrContinueStreak(UUID programId);

    /**
     * Ngắt một chuỗi và trả về bản ghi chi tiết của lần ngắt đó.
     * Được sử dụng trong luồng phục hồi streak.
     */
    StreakBreak breakStreakAndLog(UUID programId, OffsetDateTime brokenAt, UUID smokeEventId, String reason, String note);

    /**
     * Phục hồi một chuỗi đã bị ngắt trước đó.
     */
    void restoreStreak(UUID streakBreakId);
}
