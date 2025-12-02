// src/main/java/com/smokefree/program/domain/service/onboarding/OnboardingFlowServiceImpl.java
package com.smokefree.program.domain.service.onboarding;

import com.smokefree.program.domain.model.PlanTemplate;
import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.repo.PlanTemplateRepo;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.QuizService;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import com.smokefree.program.web.dto.onboarding.BaselineResultRes;
import com.smokefree.program.web.dto.onboarding.PlanOption;
import com.smokefree.program.web.dto.quiz.QuizAnswerReq;
import com.smokefree.program.web.dto.quiz.QuizAnswerRes;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.ValidationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingFlowServiceImpl implements OnboardingFlowService {

    private static final String ONBOARDING_TEMPLATE_CODE = "ONBOARDING_ASSESSMENT";

    private final QuizService quizService;
    private final SeverityRuleService severityRules;
    private final PlanTemplateRepo planTemplateRepo;
    private final BaselineResultService baselineResultService;
    private final QuizTemplateRepository quizTemplateRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public BaselineResultRes submitBaselineAndRecommend(UUID userId,
                                                        QuizAnswerReq req,
                                                        String userTier) {
        var template = quizTemplateRepository.findByCode(ONBOARDING_TEMPLATE_CODE)
                .orElseThrow(() -> new NotFoundException("Onboarding quiz template not found: " + ONBOARDING_TEMPLATE_CODE));

        int expected = template.getQuestions() == null ? 0 : template.getQuestions().size();
        if (expected <= 0) {
            throw new ValidationException("Onboarding quiz template has no questions configured");
        }
        if (req.answers() == null || req.answers().size() != expected) {
            throw new ValidationException("Must provide exactly " + expected + " answers for onboarding quiz");
        }
        var validNos = template.getQuestions().stream()
                .map(q -> q.getId().getQuestionNo())
                .collect(java.util.stream.Collectors.toSet());
        req.answers().forEach(a -> {
            if (!validNos.contains(a.q())) {
                throw new ValidationException("Invalid question number: " + a.q());
            }
        });

        // 1) Chấm điểm + severity
        QuizAnswerRes quizRes = quizService.submitAnswers(userId, req, userTier);
        int total = quizRes.total();
        SeverityLevel sev = quizRes.severity();

        // 1b) Lưu baseline (upsert) cho user, gắn với quiz template onboarding
        baselineResultService.saveOrUpdate(userId, template.getId(), total, sev);

        // 2) Map severity -> code template: L1_30D / L2_45D / L3_60D
        String recommendedCode = severityRules.recommendPlanCode(sev);

        // 3) Lấy 3 template seed
        List<String> codes = List.of("L1_30D", "L2_45D", "L3_60D");
        List<PlanTemplate> all = planTemplateRepo.findAllOrderByLevelCode();
        List<PlanTemplate> templates = all.stream()
                .filter(t -> codes.contains(t.getCode()))
                .toList();

        // 4) Lấy id template recommended
        UUID recommendedId = planTemplateRepo.findIdByCode(recommendedCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "Recommended template not found: " + recommendedCode));

        // 5) Map ra list PlanOption
        List<PlanOption> options = templates.stream()
                .map(t -> new PlanOption(
                        t.getId(),
                        t.getCode(),
                        t.getName(),
                        t.getTotalDays(),
                        t.getId().equals(recommendedId)
                ))
                .toList();

        return new BaselineResultRes(
                total,
                sev,
                recommendedId,
                recommendedCode,
                options
        );
    }
}
