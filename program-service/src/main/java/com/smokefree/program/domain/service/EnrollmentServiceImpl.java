// src/main/java/com/smokefree/program/domain/service/EnrollmentServiceImpl.java
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

    @Override
    @Transactional
    public EnrollmentRes startTrialOrPaid(UUID userId, StartEnrollmentReq req) {
        if (!baselineResultService.hasBaseline(userId)) {
            throw new ValidationException("Onboarding quiz is required before starting a program");
        }

        programRepo.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
                .ifPresent(p -> {
                    throw new ConflictException("User already has an ACTIVE program");
                });

        UUID templateId = req.planTemplateId();
        if (templateId == null) {
            throw new ValidationException("planTemplateId is required");
        }

        PlanTemplate template = planTemplateRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Plan template not found: " + templateId));

        String planCode = template.getCode();
        int planDays = template.getTotalDays();

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

        stepAssignmentService.createForProgramFromTemplate(p, template);
        assignSystemQuizzes(p);

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
                null,
                null,
                p.getStatus().name(),
                p.getStartDate() == null ? null : p.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                null,
                p.getTrialEndExpected()
        )).toList();
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

    private void assignSystemQuizzes(Program program) {
        UUID planTemplateId = program.getPlanTemplateId();
        if (planTemplateId == null) {
            return;
        }

        List<PlanQuizSchedule> schedules = planQuizScheduleRepository.findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(planTemplateId);
        for (PlanQuizSchedule schedule : schedules) {
            if (!quizAssignmentRepository.existsByTemplateIdAndProgramId(schedule.getQuizTemplateId(), program.getId())) {
                QuizAssignment assignment = new QuizAssignment();
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

                quizAssignmentRepository.save(assignment);
            }
        }
    }
}
