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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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
        // 1. Get Baseline Result (thay vì chỉ kiểm tra sự tồn tại)
        // Giả định UserBaselineResult có các getter cho severity và totalScore
        UserBaselineResult baseline = baselineResultService.latest(ownerUserId)
                .orElseThrow(() -> new ValidationException("Onboarding quiz is required before creating a program."));

        // 2. Check Active Program
        repo.findFirstByUserIdAndStatusAndDeletedAtIsNull(ownerUserId, ProgramStatus.ACTIVE)
                .ifPresent(p -> { throw new ConflictException("User already has ACTIVE program"); });

        // 3. Recommend Template based on Severity from Baseline
        PlanTemplate recommendedTemplate = findRecommendedTemplate(baseline.getSeverity());
        int planDays = recommendedTemplate.getTotalDays();

        // 4. Create Program (Trial vs Paid)
        Program p;
        // Mặc định Trial = true nếu không gửi lên (khuyến khích dùng thử)
        boolean isTrial = (req.trial() == null || Boolean.TRUE.equals(req.trial()));

        // Sử dụng planDays từ template đã được đề xuất
        if (isTrial) {
            // Hardcode trial 7 ngày hoặc config
            p = programCreationService.createTrialProgram(ownerUserId, planDays, 7, tierHeader);
        } else {
            p = programCreationService.createPaidProgram(ownerUserId, planDays, tierHeader);
        }

        // 5. Link Template Info & Baseline Results
        p.setPlanTemplateId(recommendedTemplate.getId());
        p.setTemplateCode(recommendedTemplate.getCode());
        p.setTemplateName(recommendedTemplate.getName());
        p.setCoachId(req.coachId());
        // GÁN KẾT QUẢ TỪ BASELINE VÀO PROGRAM
        p.setTotalScore(baseline.getTotalScore());
        p.setSeverity(baseline.getSeverity());

        p = repo.save(p);

        // 6. Assign Steps & Quizzes
        stepAssignmentService.createForProgramFromTemplate(p, recommendedTemplate);
        assignSystemQuizzes(p);

        log.info("Created Program {} for user {} (Trial: {})", p.getId(), ownerUserId, isTrial);

        return toRes(p, "ACTIVE", null, tierHeader);
    }

    private PlanTemplate findRecommendedTemplate(SeverityLevel severity) {
        String templateCode;
        switch (severity) {
            case LOW:
                templateCode = "L1_30D";
                break;
            case MODERATE:
                templateCode = "L2_45D";
                break;
            case HIGH:
                templateCode = "L3_60D";
                break;
            default:
                throw new IllegalStateException("Unsupported severity level for template recommendation: " + severity);
        }
        return planTemplateRepo.findByCode(templateCode)
                .orElseThrow(() -> new NotFoundException("Recommended plan template not found for code: " + templateCode));
    }

    private void assignSystemQuizzes(Program program) {
        UUID planTemplateId = program.getPlanTemplateId();
        if (planTemplateId == null) {
            log.warn("Program {} has no PlanTemplateId, skipping quiz assignment.", program.getId());
            return;
        }

        List<PlanQuizSchedule> schedules = planQuizScheduleRepo.findByPlanTemplateIdOrderByStartOffsetDayAscOrderNoAsc(planTemplateId);

        // Tối ưu hóa: Lấy tất cả các assignment đã tồn tại của program này một lần
        Set<UUID> existingTemplateIds = quizAssignmentRepo.findByProgramId(program.getId())
                .stream()
                .map(QuizAssignment::getTemplateId)
                .collect(Collectors.toSet());

        List<QuizAssignment> newAssignments = new java.util.ArrayList<>();

        for (PlanQuizSchedule schedule : schedules) {
            // Kiểm tra trong bộ nhớ thay vì query lại DB
            if (!existingTemplateIds.contains(schedule.getQuizTemplateId())) {
                QuizAssignment assignment = new QuizAssignment();
                assignment.setId(UUID.randomUUID());
                assignment.setProgramId(program.getId());
                assignment.setTemplateId(schedule.getQuizTemplateId());

                // === BỔ SUNG CÁC TRƯỜNG CÒN THIẾU ===
                assignment.setAssignedByUserId(program.getUserId());
                assignment.setCreatedBy(program.getUserId());
                // ===================================

                assignment.setStartOffsetDay(schedule.getStartOffsetDay());
                assignment.setOrderNo(schedule.getOrderNo());

                int everyDays = (schedule.getEveryDays() == null) ? 0 : schedule.getEveryDays();
                boolean isRecurring = everyDays > 0;

                assignment.setScope(isRecurring ? AssignmentScope.WEEK : AssignmentScope.ONCE);
                assignment.setEveryDays(isRecurring ? everyDays : 0);
                assignment.setPeriodDays(everyDays); // Gán giá trị cho periodDays
                assignment.setOrigin(isRecurring ? QuizAssignmentOrigin.AUTO_WEEKLY : QuizAssignmentOrigin.SYSTEM_ONBOARDING);
                assignment.setActive(true);
                assignment.setCreatedAt(Instant.now());

                // === BỔ SUNG LOGIC TÍNH NGÀY HẾT HẠN (EXPIRES_AT) ===
                LocalDate programStartDate = program.getStartDate();
                if (programStartDate != null && !isRecurring) { // Chỉ áp dụng cho quiz làm 1 lần
                    final int validForDays = 7; // Mặc định hiệu lực 7 ngày
                    LocalDate availableDate = programStartDate.plusDays(schedule.getStartOffsetDay() - 1);
                    Instant expirationInstant = availableDate.plusDays(validForDays).atStartOfDay(ZoneOffset.UTC).toInstant();

                    // Chuyển đổi sang OffsetDateTime để khớp với kiểu dữ liệu của entity
                    OffsetDateTime expiration = expirationInstant.atOffset(ZoneOffset.UTC);
                    assignment.setExpiresAt(expiration);
                }
                // =================================================

                newAssignments.add(assignment);
            }
        }

        if (!newAssignments.isEmpty()) {
            quizAssignmentRepo.saveAll(newAssignments);
            log.info("Created {} new quiz assignments for program {}", newAssignments.size(), program.getId());
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