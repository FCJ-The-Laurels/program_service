package com.smokefree.program.domain.service;

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
}
