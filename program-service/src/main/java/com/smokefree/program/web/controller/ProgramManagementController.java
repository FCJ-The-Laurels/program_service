package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.ProgramStatus;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Quản lý Program: upgrade trial → paid, end, pause, resume, update current day, v.v.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/programs")
@Slf4j
public class ProgramManagementController {

    private final ProgramRepository programRepository;

    /**
     * Upgrade từ trial → paid (sau khi payment service xác nhận).
     * Payment service sẽ gọi endpoint này sau khi thanh toán OK.
     */
    @PostMapping("/{id}/upgrade-from-trial")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PAYMENT_SERVICE')")
    public ProgramRes upgradeFromTrial(
            @PathVariable UUID id
            ) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Kiểm tra đang ở trạng thái trial
        if (program.getTrialStartedAt() == null || program.getTrialEndExpected() == null) {
            throw new ConflictException("Program is not a trial program");
        }

        if (!program.getStatus().equals(ProgramStatus.ACTIVE)) {
            throw new ConflictException("Program is not active");
        }

        // Xóa thông tin trial
        program.setTrialStartedAt(null);
        program.setTrialEndExpected(null);

        // Có thể thêm field paymentId để track payment
        // program.setPaymentId(req.paymentId());

        program = programRepository.save(program);

        log.info("[Program] User {} upgraded from trial. Program: {}", program.getUserId(), id);

        return toRes(program, null, null, null);
    }

    /**
     * Lấy trạng thái trial hiện tại.
     */
    @GetMapping("/{id}/trial-status")
    @PreAuthorize("isAuthenticated()")
    public TrialStatusRes getTrialStatus(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Kiểm tra owner
        UUID userId = SecurityUtil.requireUserId();
        if (!program.getUserId().equals(userId)) {
            throw new ConflictException("Access denied");
        }

        boolean isTrialPeriod = program.getTrialStartedAt() != null && program.getTrialEndExpected() != null;

        if (!isTrialPeriod) {
            return new TrialStatusRes(false, null, null, null, false);
        }

        long remainingDays = ChronoUnit.DAYS.between(Instant.now(), program.getTrialEndExpected());
        boolean canUpgradeNow = remainingDays > 0;

        return new TrialStatusRes(
                true,
                program.getTrialStartedAt(),
                program.getTrialEndExpected(),
                (int) remainingDays,
                canUpgradeNow
        );
    }

    /**
     * Kết thúc chương trình sớm.
     */
    @PostMapping("/{id}/end")
    @PreAuthorize("isAuthenticated()")
    public ProgramRes endProgram(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Check owner
        UUID userId = SecurityUtil.requireUserId();
        if (!program.getUserId().equals(userId)) {
            throw new ConflictException("Access denied");
        }

        if (program.getStatus() == ProgramStatus.COMPLETED || program.getStatus() == ProgramStatus.CANCELLED) {
            throw new ConflictException("Program is already finished");
        }

        program.setStatus(ProgramStatus.COMPLETED);
        program = programRepository.save(program);

        log.info("[Program] User {} ended program early. Program: {}", userId, id);

        return toRes(program, null, null, null);
    }

    /**
     * Pause chương trình (tạm dừng lộ trình).
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("isAuthenticated()")
    public ProgramRes pauseProgram(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Check owner
        UUID userId = SecurityUtil.requireUserId();
        if (!program.getUserId().equals(userId)) {
            throw new ConflictException("Access denied");
        }

        if (program.getStatus() != ProgramStatus.ACTIVE) {
            throw new ConflictException("Program is not active");
        }

        program.setStatus(ProgramStatus.PAUSED);
        program = programRepository.save(program);

        log.info("[Program] User {} paused program. Program: {}", userId, id);

        return toRes(program, null, null, null);
    }

    /**
     * Resume chương trình (tiếp tục lộ trình).
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("isAuthenticated()")
    public ProgramRes resumeProgram(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Check owner
        UUID userId = SecurityUtil.requireUserId();
        if (!program.getUserId().equals(userId)) {
            throw new ConflictException("Access denied");
        }

        if (program.getStatus() != ProgramStatus.PAUSED) {
            throw new ConflictException("Program is not paused");
        }

        program.setStatus(ProgramStatus.ACTIVE);
        program = programRepository.save(program);

        log.info("[Program] User {} resumed program. Program: {}", userId, id);

        return toRes(program, null, null, null);
    }

    /**
     * Cập nhật ngày hiện tại (chuyển sang ngày tiếp theo).
     */
    @PatchMapping("/{id}/current-day")
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramRes updateCurrentDay(
            @PathVariable UUID id,
            @RequestBody UpdateCurrentDayReq req) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        if (req.currentDay() < 1 || req.currentDay() > program.getPlanDays()) {
            throw new ConflictException("Invalid currentDay: must be between 1 and " + program.getPlanDays());
        }

        program.setCurrentDay(req.currentDay());
        program = programRepository.save(program);

        log.info("[Program] Updated current day to {} for program {}", req.currentDay(), id);

        return toRes(program, null, null, null);
    }

    /**
     * Extend trial thêm N ngày.
     */
    @PostMapping("/{id}/extend-trial")
    @PreAuthorize("hasRole('ADMIN')")
    public TrialStatusRes extendTrial(
            @PathVariable UUID id,
            @RequestBody ExtendTrialReq req) {

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        if (program.getTrialEndExpected() == null) {
            throw new ConflictException("Program is not a trial");
        }

        Instant newEndDate = program.getTrialEndExpected().plus(req.additionalDays(), ChronoUnit.DAYS);
        program.setTrialEndExpected(newEndDate);
        program = programRepository.save(program);

        long remainingDays = ChronoUnit.DAYS.between(Instant.now(), newEndDate);

        log.info("[Program] Extended trial for program {} by {} days. New end: {}",
                id, req.additionalDays(), newEndDate);

        return new TrialStatusRes(true, program.getTrialStartedAt(), newEndDate, (int) remainingDays, true);
    }

    /**
     * Get program progress (chi tiết tiến độ).
     */
    @GetMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ProgramProgressRes getProgress(@PathVariable UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Program not found: " + id));

        // Check owner
        UUID userId = SecurityUtil.requireUserId();
        if (!program.getUserId().equals(userId)) {
            throw new ConflictException("Access denied");
        }

        double percentComplete = (double) program.getCurrentDay() / program.getPlanDays() * 100;
        int daysRemaining = Math.max(0, program.getPlanDays() - program.getCurrentDay());

        // TODO: Lấy stepsCompleted, stepsTotal từ StepAssignmentService
        int stepsCompleted = 0;
        int stepsTotal = 0;

        Integer trialRemainingDays = null;
        if (program.getTrialEndExpected() != null) {
            trialRemainingDays = (int) ChronoUnit.DAYS.between(Instant.now(), program.getTrialEndExpected());
        }

        return new ProgramProgressRes(
                program.getId(),
                program.getStatus(),
                program.getCurrentDay(),
                program.getPlanDays(),
                percentComplete,
                daysRemaining,
                stepsCompleted,
                stepsTotal,
                program.getStreakCurrent(),
                trialRemainingDays
        );
    }

    // Helper
    private ProgramRes toRes(Program p, String entState, java.time.Instant entExp, String tier) {
        String effectiveTier = (tier == null) ? "basic" : tier;
        java.util.List<String> features;
        if ("vip".equalsIgnoreCase(effectiveTier)) {
            features = java.util.List.of("forum", "coach-1-1");
        } else if ("premium".equalsIgnoreCase(effectiveTier)) {
            features = java.util.List.of("forum");
        } else {
            features = java.util.Collections.emptyList();
        }
        var ent = new Entitlements(effectiveTier, features);
        var access = new Access(entState, entExp, tier);
        return new ProgramRes(
                p.getId(), p.getStatus(), p.getPlanDays(), p.getStartDate(),
                p.getCurrentDay(), p.getSeverity(), p.getTotalScore(), ent, access
        );
    }
}

