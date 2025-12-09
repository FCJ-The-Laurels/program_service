package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.BadgeService;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final ProgramRepository programRepository;
    private final StreakRepository streakRepository;
    private final BadgeService badgeService;

    /**
     * Dịch chuyển thời gian của Program về quá khứ để giả lập trôi qua N ngày.
     */
    @PostMapping("/programs/{id}/time-travel")
    @Transactional
    public String timeTravel(@PathVariable UUID id, @RequestParam int days) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found"));

        // 1. Lùi ngày bắt đầu (giả lập program đã bắt đầu từ quá khứ)
        LocalDate newStartDate = program.getStartDate().minusDays(days);
        program.setStartDate(newStartDate);

        // 2. Tăng currentDay tương ứng (nhưng không vượt quá planDays)
        int newCurrentDay = program.getCurrentDay() + days;
        if (newCurrentDay > program.getPlanDays()) newCurrentDay = program.getPlanDays();
        program.setCurrentDay(newCurrentDay);

        // 3. Lùi ngày tạo (để logic sort không bị sai)
        program.setCreatedAt(program.getCreatedAt().minus(days, ChronoUnit.DAYS));

        // 4. Nếu có Streak đang chạy, cũng phải lùi ngày bắt đầu streak
        streakRepository.findFirstByProgramIdAndEndedAtIsNullOrderByStartedAtDesc(id)
                .ifPresent(streak -> {
                    streak.setStartedAt(streak.getStartedAt().minus(days, ChronoUnit.DAYS));
                    streakRepository.save(streak);
                });

        programRepository.save(program);
        
        return String.format("Travelled %d days into the future. Current Day is now %d. Start Date is now %s.", 
                days, newCurrentDay, newStartDate);
    }

    /**
     * Reset Program về trạng thái ban đầu (Ngày 1).
     */
    @PostMapping("/programs/{id}/reset")
    @Transactional
    public String resetProgram(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found"));

        // Fix Timezone: Use UTC
        program.setStartDate(LocalDate.now(ZoneOffset.UTC));
        program.setCurrentDay(1);
        program.setCreatedAt(Instant.now());
        program.setStatus(ProgramStatus.ACTIVE);
        program.setHasPaused(false);
        program.setLastSmokeAt(null);
        
        streakRepository.deleteAllByProgramId(id);
        
        programRepository.save(program);
        return "Program reset to Day 1. Streaks cleared. StartDate set to UTC Now.";
    }
}
