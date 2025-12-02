package com.smokefree.program.domain.service.quiz.impl;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.*;
import com.smokefree.program.domain.service.quiz.QuizFlowService;
import com.smokefree.program.domain.service.smoke.StreakService;
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
    private final StreakBreakRepository streakBreakRepository;
    private final StreakService streakService; // Thêm StreakService

    @Override
    @Transactional(readOnly = true)
    public List<DueItem> listDue(UUID userId) {
        log.info("[QuizFlow] listDue for userId: {}", userId);

        Program program = programRepository
            .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
            .orElse(null);

        if (program == null) {
            log.warn("[QuizFlow] No active program found for userId: {}", userId);
            return List.of();
        }
        log.info("[QuizFlow] Found active program: {}, currentDay: {}", program.getId(), program.getCurrentDay());


        List<QuizAssignment> assignments = quizAssignmentRepository.findActiveSortedByStartOffset(program.getId());
        log.info("[QuizFlow] Found {} active assignments for program.", assignments.size());
        
        record DueCandidate(QuizAssignment assignment, DueItem item) {}
        List<DueCandidate> result = new ArrayList<>();
        Instant now = Instant.now();

        for (var assignment : assignments) {
            log.debug("[QuizFlow] Checking assignment for templateId: {}, origin: {}, startOffset: {}", 
                      assignment.getTemplateId(), assignment.getOrigin(), assignment.getStartOffsetDay());

            boolean isDue = isQuizDue(assignment, program, now);
            log.debug("[QuizFlow] isQuizDue returned: {}", isDue);


            if (isDue) {
                boolean alreadyTaken = quizResultRepository.existsByProgramIdAndTemplateId(program.getId(), assignment.getTemplateId());
                log.debug("[QuizFlow] alreadyTaken check returned: {}", alreadyTaken);

                
                // Bỏ qua quiz ONCE đã làm, TRỪ KHI nó là quiz đặc biệt có thể làm lại
                boolean isRepeatable = (assignment.getOrigin() == QuizAssignmentOrigin.STREAK_RECOVERY);
                if (assignment.getScope() == AssignmentScope.ONCE && alreadyTaken && !isRepeatable) {
                    log.info("[QuizFlow] Skipping non-repeatable ONCE quiz that is already taken. TemplateId: {}", assignment.getTemplateId());
                    continue;
                }

                QuizTemplate template = quizTemplateRepository.findById(assignment.getTemplateId()).orElse(null);
                if (template != null) {
                    Instant displayDueDate = calculateDisplayDueDate(assignment, program);
                    boolean isOverdue = !displayDueDate.isAfter(now);
                    log.info("[QuizFlow] Adding quiz to due list. TemplateId: {}", template.getId());
                    result.add(new DueCandidate(
                            assignment,
                            new DueItem(template.getId(), template.getName(), displayDueDate, isOverdue)
                    ));
                } else {
                    log.warn("[QuizFlow] QuizTemplate not found for assignment with templateId: {}", assignment.getTemplateId());
                }
            }
        }

        Set<UUID> seenTemplates = new HashSet<>();

        return result.stream()
                .sorted(Comparator
                        .comparing((DueCandidate c) -> Optional.ofNullable(c.assignment().getStartOffsetDay()).orElse(0))
                        .thenComparing(c -> Optional.ofNullable(c.assignment().getOrderNo()).orElse(0))
                        .thenComparing(c -> c.item().dueAt()))
                .filter(c -> seenTemplates.add(c.assignment().getTemplateId())) // tránh trùng template
                .map(DueCandidate::item)
                .toList();
    }

    private boolean isQuizDue(QuizAssignment assignment, Program program, Instant now) {
        int startOffset = Optional.ofNullable(assignment.getStartOffsetDay()).orElse(0);
        log.debug("[isQuizDue] Checking: startOffset={}, program.currentDay={}", startOffset, program.getCurrentDay());
        if (startOffset > 0 && program.getCurrentDay() < startOffset) {
            log.debug("[isQuizDue] Result: false (currentDay < startOffset)");
            return false;
        }

        int interval = Optional.ofNullable(assignment.getEveryDays()).orElse(0);
        if (interval > 0) {
            Instant dueDate = calculateDisplayDueDate(assignment, program);
            boolean isAfter = !dueDate.isAfter(now);
            log.debug("[isQuizDue] Interval check: dueDate={}, now={}, isDue={}", dueDate, now, isAfter);
            return isAfter;
        }

        log.debug("[isQuizDue] Result: true (ONCE or no interval)");
        return true;
    }

    private Instant calculateDisplayDueDate(QuizAssignment assignment, Program program) {
        int startOffset = Optional.ofNullable(assignment.getStartOffsetDay()).orElse(0);
        int intervalDays = Optional.ofNullable(assignment.getEveryDays()).orElse(0);

        // Nếu có lặp lại
        if (intervalDays > 0) {
            Instant lastResultDate = quizResultRepository
                .findFirstByProgramIdAndTemplateIdOrderByCreatedAtDesc(program.getId(), assignment.getTemplateId())
                .map(QuizResult::getCreatedAt)
                .orElse(program.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant());

            int everyDays = intervalDays > 0 ? intervalDays : 7;
            Instant candidate = lastResultDate.plus(Duration.ofDays(everyDays));

            if (startOffset > 0) {
                Instant earliest = program.getStartDate()
                        .plusDays(startOffset - 1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
                return candidate.isBefore(earliest) ? earliest : candidate;
            }
            return candidate;
        }

        // ONCE: due từ ngày offset trong kế hoạch
        if (startOffset > 0) {
            return program.getStartDate()
                    .plusDays(startOffset - 1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
        }

        // Fallback
        return program.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Override
    @Transactional
    public OpenAttemptRes openAttempt(UUID userId, UUID templateId) {
        log.info("[QuizFlow] openAttempt for userId: {}, templateId: {}", userId, templateId);

        Program program = programRepository
            .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("No active program for user: " + userId));
        
        if (program.getTrialEndExpected() != null && Instant.now().isAfter(program.getTrialEndExpected())) {
             throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        QuizAssignment assignment = quizAssignmentRepository
                .findActiveByProgramAndTemplate(program.getId(), templateId)
                .orElseThrow(() -> new ForbiddenException("Template not assigned to your program"));

        boolean isRepeatable = (assignment.getOrigin() == QuizAssignmentOrigin.STREAK_RECOVERY);
        if (assignment.getScope() == AssignmentScope.ONCE &&
                quizResultRepository.existsByProgramIdAndTemplateId(program.getId(), templateId) && !isRepeatable) {
            throw new ConflictException("Quiz already completed");
        }

        if (!isQuizDue(assignment, program, Instant.now())) {
            throw new ConflictException("Quiz is not due yet");
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

    @Override
    @Transactional
    public void saveAnswer(UUID userId, UUID attemptId, AnswerReq req) {
        log.info("[QuizFlow] saveAnswer - userId: {}, attemptId: {}, question: {}", userId, attemptId, req.questionNo());

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new NotFoundException("Attempt not found"));

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
        
        UUID templateId = attempt.getTemplateId();
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

        // === LOGIC NGHIỆP VỤ ĐẶC BIỆT: XỬ LÝ SAU KHI NỘP QUIZ ===
        handlePostSubmissionActions(attempt);

        return new SubmitRes(attempt.getId(), totalScore, severity.name());
    }

    private void handlePostSubmissionActions(QuizAttempt attempt) {
        QuizAssignment assignment = quizAssignmentRepository
                .findActiveByProgramAndTemplate(attempt.getProgramId(), attempt.getTemplateId())
                .orElse(null);

        if (assignment == null) {
            return; // Không tìm thấy assignment, không xử lý gì thêm
        }

        // 1. Xử lý cho quiz "STREAK_RECOVERY"
        if (assignment.getOrigin() == QuizAssignmentOrigin.STREAK_RECOVERY) {
            log.info("[QuizFlow] Handling STREAK_RECOVERY post-submission for program: {}", attempt.getProgramId());
            
            // Tìm lần phá vỡ streak gần nhất để lấy ID của nó
            List<StreakBreak> breaks = streakBreakRepository
                    .findByProgramIdOrderByBrokenAtDesc(attempt.getProgramId());
            StreakBreak lastBreak = breaks.isEmpty() ? null : breaks.get(0);

            if (lastBreak != null) {
                log.info("[QuizFlow] Found last streak break {}. Calling StreakService to restore.", lastBreak.getId());
                streakService.restoreStreak(lastBreak.getId());
                
                // Tăng số lần sử dụng
                Program program = programRepository.findById(attempt.getProgramId()).orElse(null);
                if (program != null) {
                    program.setStreakRecoveryUsedCount(program.getStreakRecoveryUsedCount() + 1);
                    programRepository.save(program);
                }

            } else {
                log.warn("[QuizFlow] No streak break found for program {}. Cannot restore streak.", attempt.getProgramId());
            }
        }

        // (Có thể thêm các xử lý khác cho các loại quiz khác ở đây trong tương lai)
    }
}
