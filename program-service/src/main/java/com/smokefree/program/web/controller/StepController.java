package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import com.smokefree.program.web.dto.step.RescheduleStepReq;
import com.smokefree.program.web.dto.step.UpdateStepStatusReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * StepController - Quản lý toàn bộ bài tập (steps) của program.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/programs/{programId}/steps")
public class StepController {

    private final StepAssignmentService service;

    // --- LIST & GET ---

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<StepAssignment> list(@PathVariable UUID programId) {
        log.info("[Step] LIST programId={}", programId);
        return service.listByProgram(programId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public StepAssignment get(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] GET programId={}, id={}", programId, id);
        return service.getOne(programId, id);
    }

    /**
     * Lấy danh sách step của ngày hôm nay.
     */
    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    public List<StepAssignment> getTodaySteps(@PathVariable UUID programId) {
        UUID userId = SecurityUtil.requireUserId();
        log.info("[Step] Get TODAY steps for program {} user {}", programId, userId);

        List<StepAssignment> allSteps = service.listByProgram(programId);
        int todayDayOfYear = LocalDate.now(ZoneOffset.UTC).getDayOfYear();

        return allSteps.stream()
                .filter(s -> s.getPlannedDay() != null && s.getPlannedDay().equals(todayDayOfYear))
                .toList();
    }

    // --- CREATE & MANAGE ---

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public StepAssignment create(@PathVariable UUID programId,
                                 @RequestBody @Valid CreateStepAssignmentReq req) {
        log.info("[Step] CREATE programId={}, body={}", programId, req);
        return service.create(programId, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void updateStatus(@PathVariable("programId") UUID programId,
                             @PathVariable("id") UUID assignmentId,
                             @RequestBody UpdateStepStatusReq req) {

        UUID userId = SecurityUtil.requireUserId();
        service.updateStatus(userId, programId, assignmentId, req.status(), req.note());
    }

    /**
     * Skip một step.
     */
    @PostMapping("/{id}/skip")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public StepAssignment skipStep(@PathVariable UUID programId, @PathVariable UUID id) {
        UUID userId = SecurityUtil.requireUserId();
        log.info("[Step] SKIP step {} program {}", id, programId);
        service.updateStatus(userId, programId, id, StepStatus.SKIPPED, "User skipped");
        return service.getOne(programId, id);
    }

    /**
     * Reschedule step (lặp lại lịch) và lưu DB.
     */
    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public StepAssignment rescheduleStep(@PathVariable UUID programId,
                                         @PathVariable UUID id,
                                         @RequestBody RescheduleStepReq req) {
        SecurityUtil.requireUserId();
        log.info("[Step] RESCHEDULE step {} to {}", id, req.newScheduledAt());
        return service.reschedule(programId, id, req.newScheduledAt());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable UUID programId, @PathVariable UUID id) {
        log.info("[Step] DELETE programId={}, id={}", programId, id);
        service.delete(programId, id);
    }
}
