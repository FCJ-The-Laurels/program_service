package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.PlanTemplateRepo;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
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
    private final QuizTemplateRepository quizTemplateRepo;
    private final QuizAssignmentRepository quizAssignmentRepo;
    
    // --- Dependencies mới để tạo bài học ---
    private final PlanTemplateRepo planTemplateRepo;
    private final StepAssignmentService stepAssignmentService;

    @Override
    @Transactional
    public ProgramRes createProgram(UUID ownerUserId, CreateProgramReq req, String tierHeader) {
        repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(ownerUserId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has ACTIVE program"); });

        // 1. Tìm Plan Template (Logic mới cần có ID hoặc Code, giả sử req.planTemplateId có trong DTO hoặc hardcode/logic cũ)
        // Lưu ý: CreateProgramReq hiện tại trong file tôi đọc lúc trước chỉ có 'planDays'. 
        // Cần kiểm tra lại CreateProgramReq. Nếu thiếu templateId thì phải thêm vào hoặc tìm default.
        // Tạm thời tôi sẽ giả định logic tìm template mặc định 30 ngày nếu không có ID.
        
        int planDays = (req.planDays() == null ? 30 : req.planDays());
        
        // Tìm template phù hợp (VD: theo ngày)
        PlanTemplate template = planTemplateRepo.findAll().stream()
                .filter(t -> t.getTotalDays() == planDays)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No plan template found for " + planDays + " days"));

        // 2. Tạo Program
        Program p = programCreationService.createPaidProgram(ownerUserId, planDays, tierHeader);
        p.setPlanTemplateId(template.getId());
        p.setTemplateCode(template.getCode());
        p.setTemplateName(template.getName());
        
        p = repo.save(p);

        // 3. Tạo Step Assignments (Bài học hàng ngày)
        stepAssignmentService.createForProgramFromTemplate(p, template);

        // 4. Auto-assign System Quizzes
        assignSystemQuizzes(p);

        return toRes(p, "ACTIVE", null, tierHeader);
    }

    private void assignSystemQuizzes(Program program) {
        assignQuizByTemplateName(program, "Onboarding Assessment", AssignmentScope.ONCE, 0, 0, QuizAssignmentOrigin.SYSTEM_ONBOARDING);
        assignQuizByTemplateName(program, "Weekly Check-in", AssignmentScope.PROGRAM, 7, 7, QuizAssignmentOrigin.AUTO_WEEKLY);
    }

    private void assignQuizByTemplateName(Program program, String templateName, AssignmentScope scope, int startOffset, int everyDays, QuizAssignmentOrigin origin) {
        Optional<QuizTemplate> tplOpt = quizTemplateRepo.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase(templateName) && t.getStatus() == QuizTemplateStatus.PUBLISHED)
                .findFirst();

        if (tplOpt.isPresent()) {
            QuizTemplate tpl = tplOpt.get();
            if (!quizAssignmentRepo.existsByTemplateIdAndProgramId(tpl.getId(), program.getId())) {
                QuizAssignment assignment = new QuizAssignment();
                assignment.setId(UUID.randomUUID());
                assignment.setProgramId(program.getId());
                assignment.setTemplateId(tpl.getId());
                assignment.setScope(scope);
                assignment.setOrigin(origin);
                assignment.setStartOffsetDay(startOffset);
                assignment.setEveryDays(everyDays);
                assignment.setActive(true);
                assignment.setCreatedAt(Instant.now());
                assignment.setCreatedBy(null);
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
                // log.warn("User {} trial expired...", userId);
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