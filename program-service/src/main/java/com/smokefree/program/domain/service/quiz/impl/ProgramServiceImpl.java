package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.ProgramCreationService;
import com.smokefree.program.domain.service.ProgramService;
import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.SubscriptionRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository repo;
    private final ProgramCreationService programCreationService;
    private final QuizAssignmentRepository quizAssignmentRepo;
    private final PlanTemplateRepo planTemplateRepo;
    private final StepAssignmentService stepAssignmentService;
    private final PlanQuizScheduleRepository planQuizScheduleRepo; // Thêm dependency mới

    @Override
    @Transactional
    public ProgramRes createProgram(UUID ownerUserId, CreateProgramReq req, String tierHeader) {
        repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(ownerUserId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has ACTIVE program"); });

        int planDays = (req.planDays() == null ? 30 : req.planDays());
        
        PlanTemplate template = planTemplateRepo.findAll().stream()
                .filter(t -> t.getTotalDays() == planDays)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No plan template found for " + planDays + " days"));

        Program p = programCreationService.createPaidProgram(ownerUserId, planDays, tierHeader);
        p.setPlanTemplateId(template.getId());
        p.setTemplateCode(template.getCode());
        p.setTemplateName(template.getName());
        
        p = repo.save(p);

        stepAssignmentService.createForProgramFromTemplate(p, template);

        // Gọi logic gán quiz đã được sửa đổi
        assignSystemQuizzes(p);

        return toRes(p, "ACTIVE", null, tierHeader);
    }

    /**
     * Logic mới: Gán quiz một cách linh hoạt dựa trên cấu hình của PlanTemplate.
     */
    private void assignSystemQuizzes(Program program) {
        UUID planTemplateId = program.getPlanTemplateId();
        if (planTemplateId == null) {
            log.warn("Program {} has no PlanTemplateId, skipping quiz assignment.", program.getId());
            return;
        }

        // 1. Tìm tất cả "thời khóa biểu" quiz đã được Admin cấu hình cho Plan Template này
        List<PlanQuizSchedule> schedules = planQuizScheduleRepo.findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(planTemplateId);

        // 2. Lặp qua từng "thời khóa biểu" và tạo "lịch hẹn" (QuizAssignment) cho người dùng
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
}
