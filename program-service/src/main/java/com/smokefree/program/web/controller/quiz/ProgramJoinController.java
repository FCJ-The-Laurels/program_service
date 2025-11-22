package com.smokefree.program.web.controller.quiz;

import com.smokefree.program.domain.service.EnrollmentService;
import com.smokefree.program.util.SecurityUtil;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/programs")
@RequiredArgsConstructor
public class ProgramJoinController {

    private final EnrollmentService enrollmentService;

    /**
     * User join 1 plan template → tạo Program mới (trial hoặc paid).
     * Path variable {programId} chính là planTemplateId.
     */
    @PostMapping("/{programId}/join")
    @PreAuthorize("hasRole('USER')")
    public EnrollmentRes joinProgram(
            @PathVariable UUID programId,
            @RequestBody(required = false) JoinProgramReq body
    ) {
        UUID userId = SecurityUtil.requireUserId();
        boolean trial = body == null || Boolean.TRUE.equals(body.trial());

        // Dùng lại logic sẵn có trong EnrollmentService
        StartEnrollmentReq req = new StartEnrollmentReq(programId, trial);
        return enrollmentService.startTrialOrPaid(userId, req);
    }

    // Body tối giản: chỉ cần flag trial
    public record JoinProgramReq(Boolean trial) {}
}
