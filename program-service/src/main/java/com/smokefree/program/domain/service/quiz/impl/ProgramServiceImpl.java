package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.ProgramCreationService;
import com.smokefree.program.domain.service.ProgramService;
import com.smokefree.program.domain.service.onboarding.BaselineResultService;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.SubscriptionRequiredException;
import com.smokefree.program.web.error.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository repo;
    private final ProgramCreationService programCreationService;
    private final QuizAssignmentRepository quizAssignmentRepo;
    private final PlanTemplateRepo planTemplateRepo;
    private final StepAssignmentService stepAssignmentService;
    private final PlanQuizScheduleRepository planQuizScheduleRepo;
    private final BaselineResultService baselineResultService;

    @Override
    @Transactional
    public ProgramRes createProgram(UUID ownerUserId, CreateProgramReq req, String tierHeader) {
        // 1. Validate Baseline
        if (!baselineResultService.hasBaseline(ownerUserId)) {
            throw new ValidationException("Onboarding quiz is required before creating a program");
        }

        // 2. Check Active Program
        repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(ownerUserId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has ACTIVE program"); });

        // 3. Find Template
        PlanTemplate template;
        if (req.planTemplateId() != null) {
            // Ưu tiên tìm theo ID chính xác
            template = planTemplateRepo.findById(req.planTemplateId())
                    .orElseThrow(() -> new NotFoundException("Plan template not found: " + req.planTemplateId()));
        } else {
            // Fallback: Tìm theo days (Logic cũ)
            int planDays = (req.planDays() == null ? 30 : req.planDays());
            template = planTemplateRepo.findAll().stream()
                    .filter(t -> t.getTotalDays() == planDays)
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("No plan template found for " + planDays + " days"));
        }

        int planDays = template.getTotalDays();

        // 4. Create Program (Trial vs Paid)
        Program p;
        // Mặc định Trial = true nếu không gửi lên (khuyến khích dùng thử)
        boolean isTrial = (req.trial() == null || Boolean.TRUE.equals(req.trial()));

        if (isTrial) {
            // Hardcode trial 7 ngày hoặc config
            p = programCreationService.createTrialProgram(ownerUserId, planDays, 7, tierHeader);
        } else {
            p = programCreationService.createPaidProgram(ownerUserId, planDays, tierHeader);
        }

        // 5. Link Template Info
        p.setPlanTemplateId(template.getId());
        p.setTemplateCode(template.getCode());
        p.setTemplateName(template.getName());
        p.setCoachId(req.coachId());

        p = repo.save(p);

        // 6. Assign Steps & Quizzes
        stepAssignmentService.createForProgramFromTemplate(p, template);
        assignSystemQuizzes(p);

        log.info("Created Program {} for user {} (Trial: {})", p.getId(), ownerUserId, isTrial);

        return toRes(p, "ACTIVE", null, tierHeader);
    }

    private void assignSystemQuizzes(Program program) {
        UUID planTemplateId = program.getPlanTemplateId();
        if (planTemplateId == null) {
            log.warn("Program {} has no PlanTemplateId, skipping quiz assignment.", program.getId());
            return;
        }

        List<PlanQuizSchedule> schedules = planQuizScheduleRepo.findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(planTemplateId);

        for (PlanQuizSchedule schedule : schedules) {
            if (!quizAssignmentRepo.existsByTemplateIdAndProgramId(schedule.getQuizTemplateId(), program.getId())) {
                QuizAssignment assignment = new QuizAssignment();
                assignment.setId(UUID.randomUUID());
                assignment.setProgramId(program.getId());
                assignment.setTemplateId(schedule.getQuizTemplateId());
                assignment.setStartOffsetDay(schedule.getStartOffsetDay());
                assignment.setOrderNo(schedule.getOrderNo());

                int everyDays = (schedule.getEveryDays() == null) ? 0 : schedule.getEveryDays();
                boolean isRecurring = everyDays > 0;

                assignment.setScope(isRecurring ? AssignmentScope.WEEK : AssignmentScope.ONCE);
                assignment.setEveryDays(isRecurring ? everyDays : 0);
                assignment.setOrigin(isRecurring ? QuizAssignmentOrigin.AUTO_WEEKLY : QuizAssignmentOrigin.SYSTEM_ONBOARDING);
                assignment.setActive(true);
                assignment.setCreatedAt(Instant.now());

                quizAssignmentRepo.save(assignment);
            }
        }
    }

    @Override
    public Optional<Program> getActive(UUID userId) {
        Optional<Program> pOpt = repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE);

        if (pOpt.isPresent()) {
            Program p = pOpt.get();
            if (p.getTrialEndExpected() != null && Instant.now().isAfter(p.getTrialEndExpected())) {
                throw new SubscriptionRequiredException("Your free trial has expired. Please subscribe to continue.");
            }
        }
        return pOpt;
    }

    @Override
    public ProgramRes toRes(Program p, String entState, Instant entExp, String tier) {
        String effectiveTier = (tier == null) ? "basic" : tier;
        List<String> features;
        if ("vip".equalsIgnoreCase(effectiveTier)) {
            features = List.of("forum", "coach-1-1");
        } else if ("premium".equalsIgnoreCase(effectiveTier)) {
            features = List.of("forum");
        } else {
            features = Collections.emptyList();
        }
        Entitlements ent = new Entitlements(effectiveTier, features);
        Access access = new Access(entState, entExp, tier);
        return new ProgramRes(
                p.getId(), p.getStatus(), p.getPlanDays(), p.getStartDate(),
                p.getCurrentDay(), p.getSeverity(), p.getTotalScore(), ent, access
        );
    }

    @Override
    public List<Program> listByUser(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public org.springframework.data.domain.Page<Program> listAll(int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return repo.findAll(pageable);
    }
}