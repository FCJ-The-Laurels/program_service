package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.quiz.QuizFlowService;
import com.smokefree.program.web.dto.quiz.answer.AnswerReq;
import com.smokefree.program.web.dto.quiz.attempt.DueItem;
import com.smokefree.program.web.dto.quiz.attempt.OpenAttemptRes;
import com.smokefree.program.web.dto.quiz.result.SubmitRes;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizFlowServiceImpl implements QuizFlowService {

    private final ProgramRepository programRepository;
    private final QuizAssignmentRepository quizAssignmentRepository;
    private final QuizTemplateRepository quizTemplateRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizResultRepository quizResultRepository;
    private final SeverityRuleService severityRuleService;

    // --- [KEEPING LIST DUE & OPEN ATTEMPT LOGIC AS IS] ---
    // (Để tiết kiệm không gian và vì nó đã ổn, tôi chỉ sửa saveAnswer và submit)

    @Override
    @Transactional(readOnly = true)
    public List<DueItem> listDue(UUID userId) {
        log.info("[QuizFlow] listDue for userId: {}", userId);

        Program program = programRepository
            .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
            .or(() -> programRepository.findByUserId(userId))
            .orElse(null);

        if (program == null) return List.of();

        List<QuizAssignment> assignments = quizAssignmentRepository
            .findActiveSortedByStartOffset(program.getId());
        if (assignments == null || assignments.isEmpty()) {
            assignments = quizAssignmentRepository.findByProgramId(program.getId());
        }

        List<DueItem> result = new ArrayList<>();
        Instant now = Instant.now();

        for (var assignment : assignments) {
            Instant dueAt = calculateDueDate(assignment, program);
            boolean isOverdue = isQuizDue(assignment, program, dueAt, now);

            if (isOverdue) {
                QuizTemplate template = quizTemplateRepository.findById(assignment.getTemplateId()).orElse(null);
                if (template != null) {
                    result.add(new DueItem(template.getId(), template.getName(), dueAt, isOverdue));
                }
            }
        }
        return result;
    }

    private Instant calculateDueDate(QuizAssignment assignment, Program program) {
        if (assignment.getStartOffsetDay() != null && assignment.getStartOffsetDay() > 0) {
            return program.getStartDate()
                .plusDays(assignment.getStartOffsetDay())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        }
        Instant lastResult = quizResultRepository
            .findFirstByProgramIdAndTemplateIdOrderByCreatedAtDesc(program.getId(), assignment.getTemplateId())
            .map(QuizResult::getCreatedAt)
            .orElse(program.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant());

        int everyDays = (assignment.getEveryDays() == null || assignment.getEveryDays() <= 0) ? 5 : assignment.getEveryDays();
        return lastResult.plus(Duration.ofDays(everyDays));
    }

    private boolean isQuizDue(QuizAssignment assignment, Program program, Instant dueAt, Instant now) {
        if (assignment.getStartOffsetDay() != null && assignment.getStartOffsetDay() > 0) {
            return program.getCurrentDay() >= assignment.getStartOffsetDay();
        }
        return !dueAt.isAfter(now);
    }

    @Override
    @Transactional
    public OpenAttemptRes openAttempt(UUID userId, UUID templateId) {
        log.info("[QuizFlow] openAttempt for userId: {}, templateId: {}", userId, templateId);

        Program program = programRepository
            .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
            .or(() -> programRepository.findByUserId(userId))
            .orElseThrow(() -> new NotFoundException("No active program for user: " + userId));
        
        // HARD STOP check (nếu ProgramService chưa chặn, chặn ở đây)
        if (program.getTrialEndExpected() != null && Instant.now().isAfter(program.getTrialEndExpected())) {
             throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        boolean exists = quizAssignmentRepository.existsByTemplateIdAndProgramId(templateId, program.getId());
        if (!exists) {
            throw new ForbiddenException("Template not assigned to your program");
        }

        quizAttemptRepository.findFirstByProgramIdAndTemplateIdAndStatus(
            program.getId(), templateId, AttemptStatus.OPEN
        ).ifPresent(a -> { throw new ConflictException("An attempt is already open"); });

        QuizTemplate t = quizTemplateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException("Template not found"));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setProgramId(program.getId());
        attempt.setTemplateId(t.getId());
        attempt.setUserId(userId);
        attempt.setOpenedAt(Instant.now());
        attempt.setStatus(AttemptStatus.OPEN);
        attempt.setAnswers(new ArrayList<>());
        quizAttemptRepository.save(attempt);

        List<OpenAttemptRes.QuestionView> questions = t.getQuestions().stream()
            .sorted(Comparator.comparing(q -> q.getId().getQuestionNo()))
            .map(q -> new OpenAttemptRes.QuestionView(
                q.getId().getQuestionNo(),
                q.getQuestionText(),
                q.getChoiceLabels().stream()
                    .sorted(Comparator.comparing(c -> c.getId().getLabelCode()))
                    .collect(Collectors.toMap(
                        c -> c.getId().getLabelCode(),
                        QuizChoiceLabel::getLabelText,
                        (a, b) -> a,
                        LinkedHashMap::new
                    ))
            ))
            .toList();

        return new OpenAttemptRes(attempt.getId(), t.getId(), t.getVersion(), questions);
    }

    // --- [CHANGED METHODS] ---

    @Override
    @Transactional
    public void saveAnswer(UUID userId, UUID attemptId, AnswerReq req) {
        log.info("[QuizFlow] saveAnswer - userId: {}, attemptId: {}, question: {}", userId, attemptId, req.questionNo());

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new NotFoundException("Attempt not found"));

        // Validate owner & status
        if (!attempt.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your attempt");
        }
        if (attempt.getStatus() != AttemptStatus.OPEN) {
            throw new ForbiddenException("Attempt is not OPEN");
        }

        attempt.getAnswers().removeIf(a -> a.getId().getQuestionNo().equals(req.questionNo()));

        QuizAnswerId id = new QuizAnswerId();
        id.setAttemptId(attemptId);
        id.setQuestionNo(req.questionNo());

        QuizAnswer answer = new QuizAnswer();
        answer.setId(id);
        answer.setAttempt(attempt);
        answer.setAnswer(req.answer());
        answer.setCreatedAt(Instant.now());
        attempt.getAnswers().add(answer);

        quizAttemptRepository.save(attempt);
    }

    @Override
    @Transactional
    public SubmitRes submit(UUID userId, UUID attemptId) {
        log.info("[QuizFlow] submit - userId: {}, attemptId: {}", userId, attemptId);

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new NotFoundException("Attempt not found"));

        if (!attempt.getUserId().equals(userId)) {
            throw new ForbiddenException("Not your attempt");
        }
        if (attempt.getStatus() != AttemptStatus.OPEN) {
            throw new ForbiddenException("Attempt is not OPEN");
        }

        int totalScore = attempt.getAnswers().stream().mapToInt(QuizAnswer::getAnswer).sum();
        SeverityLevel severity = severityRuleService.fromScore(totalScore);
        
        UUID templateId = attempt.getTemplateId(); // Self-lookup
        QuizTemplate template = quizTemplateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException("Template not found (data inconsistent)"));

        QuizResult result = new QuizResult();
        result.setId(UUID.randomUUID());
        result.setProgramId(attempt.getProgramId());
        result.setTemplateId(templateId);
        result.setQuizVersion(template.getVersion());
        result.setTotalScore(totalScore);
        result.setSeverity(severity);
        result.setCreatedAt(Instant.now());
        quizResultRepository.save(result);

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        quizAttemptRepository.save(attempt);

        return new SubmitRes(attempt.getId(), totalScore, severity.name());
    }
}
