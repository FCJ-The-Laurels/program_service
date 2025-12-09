package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.onboarding.BaselineResultService;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors; // Add this import

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final ProgramRepository programRepo;
    private final PlanTemplateRepo planTemplateRepo;
    private final ProgramCreationService programCreationService;
    private final StepAssignmentService stepAssignmentService;
    private final BaselineResultService baselineResultService;
    private final PlanQuizScheduleRepository planQuizScheduleRepository;
    private final QuizAssignmentRepository quizAssignmentRepository;
    private final BadgeService badgeService;

    /**
     * Starts a new program (Trial or Paid) for a user.
     * <p>
     * Flow:
     * 1. Check Baseline: User must have completed the onboarding quiz.
     * 2. Check Existing: User cannot have another ACTIVE program.
     * 3. Validate Template: Ensure the requested PlanTemplate exists.
     * 4. Create Program: Initialize Program entity (Trial vs Paid logic).
     * 5. Assign Steps: Link program to daily steps.
     * 6. Assign Quizzes: Batch create default quizzes based on the template schedule.
     * </p>
     */
    @Override
    @Transactional
    public EnrollmentRes startTrialOrPaid(UUID userId, StartEnrollmentReq req) {
        // 1. Validation: Baseline is mandatory
        if (!baselineResultService.hasBaseline(userId)) {
            throw new ValidationException("Onboarding quiz is required before starting a program");
        }

        // 2. Validation: No concurrent active programs allowed
        programRepo.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
                .ifPresent(p -> {
                    throw new ConflictException("User already has an ACTIVE program");
                });

        // 3. Load Plan Template
        UUID templateId = req.planTemplateId();
        if (templateId == null) {
            throw new ValidationException("planTemplateId is required");
        }

        PlanTemplate template = planTemplateRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Plan template not found: " + templateId));

        String planCode = template.getCode();
        int planDays = template.getTotalDays();

        // 4. Create Program Entity
        Program p;
        if (Boolean.TRUE.equals(req.trial())) {
            p = programCreationService.createTrialProgram(userId, planDays, 7, null);
        } else {
            p = programCreationService.createPaidProgram(userId, planDays, null);
        }
        p.setPlanTemplateId(template.getId());
        p.setTemplateCode(template.getCode());
        p.setTemplateName(template.getName());
        p = programRepo.save(p);

        // 5. Assign Content & Quizzes
        stepAssignmentService.createForProgramFromTemplate(p, template);
        assignSystemQuizzes(p);
        
        // Check Badge Level 1 (Start)
        badgeService.checkProgramMilestone(p);

        Instant trialUntil = p.getTrialEndExpected();
        Instant startAt = p.getStartDate() == null
                ? null
                : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();

        return new EnrollmentRes(
                p.getId(),
                p.getUserId(),
                templateId,
                planCode,
                p.getStatus().name(),
                startAt,
                null,
                trialUntil
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentRes> listByUser(UUID userId) {
        List<Program> programs = programRepo.findAllByUserId(userId);
        programs.sort(Comparator.comparing(Program::getCreatedAt).reversed());

        return programs.stream().map(p -> new EnrollmentRes(
                p.getId(),
                p.getUserId(),
                p.getPlanTemplateId(), // Added back p.getPlanTemplateId()
                p.getTemplateCode(),   // Added back p.getTemplateCode()
                p.getStatus().name(),
                p.getStartDate() == null ? null : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                null,
                p.getTrialEndExpected()
        )).collect(Collectors.toList()); // Use .collect(Collectors.toList())
    }

    @Override
    @Transactional
    public void complete(UUID userId, UUID enrollmentId) {
        Program p = programRepo.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        if (p.getStatus() == ProgramStatus.COMPLETED) {
            throw new ConflictException("Enrollment already completed");
        }
        if (p.getStatus() == ProgramStatus.CANCELLED) {
            throw new ConflictException("Enrollment is cancelled");
        }

        p.setStatus(ProgramStatus.COMPLETED);
        programRepo.save(p);
    }

    @Override
    @Transactional
    public EnrollmentRes activatePaid(UUID userId, UUID enrollmentId) {
        Program p = programRepo.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: " + enrollmentId));

        if (p.getStatus() != ProgramStatus.ACTIVE) {
            throw new ConflictException("Enrollment is not ACTIVE. Cannot activate.");
        }
        if (p.getTrialEndExpected() == null) {
            throw new ConflictException("Enrollment is not a trial program. Cannot activate.");
        }

        p.setTrialEndExpected(null);
        p = programRepo.save(p);

        return new EnrollmentRes(
                p.getId(),
                p.getUserId(),
                p.getPlanTemplateId(),
                p.getTemplateCode(),
                p.getStatus().name(),
                p.getStartDate() == null ? null : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                null,
                null
        );
    }

    /**
     * Assigns default quizzes from the PlanTemplate to the new Program.
     * <p>
     * Optimization: Uses "Batch Fetch & Filter" pattern to avoid N+1 SELECT/INSERT issues.
     * Instead of checking existence one-by-one, it loads all existing assignments into memory
     * and filters the new ones before performing a single 'saveAll'.
     * </p>
     */
    private void assignSystemQuizzes(Program program) {
        UUID planTemplateId = program.getPlanTemplateId();
        if (planTemplateId == null) {
            return;
        }

        List<PlanQuizSchedule> schedules = planQuizScheduleRepository.findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(planTemplateId);
        
        // Optimization: Fetch all existing assignments once to avoid N+1 queries
        java.util.Set<UUID> existingTemplateIds = quizAssignmentRepository.findByProgramId(program.getId())
                .stream()
                .map(com.smokefree.program.domain.model.QuizAssignment::getTemplateId)
                .collect(java.util.stream.Collectors.toSet());

        List<com.smokefree.program.domain.model.QuizAssignment> newAssignments = new java.util.ArrayList<>();

        for (PlanQuizSchedule schedule : schedules) {
            if (!existingTemplateIds.contains(schedule.getQuizTemplateId())) {
                com.smokefree.program.domain.model.QuizAssignment assignment = new com.smokefree.program.domain.model.QuizAssignment();
                assignment.setId(UUID.randomUUID());
                assignment.setProgramId(program.getId());
                assignment.setTemplateId(schedule.getQuizTemplateId());
                assignment.setStartOffsetDay(schedule.getStartOffsetDay());
                assignment.setOrderNo(schedule.getOrderNo());

                int everyDays = schedule.getEveryDays() == null ? 0 : schedule.getEveryDays();
                boolean isRecurring = everyDays > 0;

                assignment.setScope(isRecurring ? AssignmentScope.WEEK : AssignmentScope.ONCE);
                assignment.setEveryDays(isRecurring ? everyDays : 0);
                assignment.setOrigin(isRecurring ? QuizAssignmentOrigin.AUTO_WEEKLY : QuizAssignmentOrigin.SYSTEM_ONBOARDING);
                assignment.setActive(true);
                assignment.setCreatedAt(Instant.now());

                newAssignments.add(assignment);
            }
        }

        if (!newAssignments.isEmpty()) {
            quizAssignmentRepository.saveAll(newAssignments);
        }
    }
}