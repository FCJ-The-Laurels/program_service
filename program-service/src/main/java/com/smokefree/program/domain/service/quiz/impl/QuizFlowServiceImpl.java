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
    private final StreakRepository streakRepository;
    private final com.smokefree.program.domain.service.BadgeService badgeService;

    /**
     * Retrieves the list of quizzes currently due for the user.
     * <p>
     * Optimization Strategy:
     * 1. Fetch all active assignments for the program.
     * 2. Bulk fetch all quiz results (Map: TemplateId -> Latest Result) to avoid N+1 queries.
     * 3. Bulk fetch all quiz templates to avoid N+1 queries.
     * 4. Process in-memory to determine due dates based on schedule (ONCE vs Recurring).
     * </p>
     *
     * @param userId The ID of the user.
     * @return List of due items sorted by schedule order and due date.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DueItem> listDue(UUID userId) {
        log.info("[QuizFlow] listDue optimized for userId: {}", userId);

        Program program = programRepository
            .findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, ProgramStatus.ACTIVE)
            .orElse(null);

        if (program == null) {
            log.warn("[QuizFlow] No active program found for userId: {}", userId);
            return List.of();
        }
        if (program.getTrialEndExpected() != null && Instant.now().isAfter(program.getTrialEndExpected())) {
            throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        // 1. Fetch all assignments
        List<QuizAssignment> assignments = quizAssignmentRepository.findActiveSortedByStartOffset(program.getId());
        if (assignments.isEmpty()) return List.of();

        // 2. Bulk fetch all results (Map: TemplateId -> Latest Result)
        Map<UUID, QuizResult> latestResultsMap = quizResultRepository.findByProgramId(program.getId())
                .stream()
                .collect(Collectors.toMap(
                        QuizResult::getTemplateId,
                        r -> r,
                        (existing, replacement) -> existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
                ));

        // 3. Bulk fetch all templates
        Set<UUID> templateIds = assignments.stream()
                .map(QuizAssignment::getTemplateId)
                .collect(Collectors.toSet());
        Map<UUID, QuizTemplate> templateMap = quizTemplateRepository.findAllById(templateIds)
                .stream()
                .collect(Collectors.toMap(QuizTemplate::getId, t -> t));

        List<DueItem> result = new ArrayList<>();
        Instant now = Instant.now();

        for (var assignment : assignments) {
            QuizTemplate template = templateMap.get(assignment.getTemplateId());
            if (template == null) continue;

            QuizResult lastResult = latestResultsMap.get(assignment.getTemplateId());
            boolean alreadyTaken = (lastResult != null);

            // Logic check Due
            Instant displayDueDate = calculateDisplayDueDateOptimized(assignment, program, lastResult);
            boolean isDue = checkIsDueOptimized(assignment, program, now, displayDueDate);

            if (isDue) {
                // Logic ONCE check
                boolean isRepeatable = (assignment.getOrigin() == QuizAssignmentOrigin.STREAK_RECOVERY);
                if (assignment.getScope() == AssignmentScope.ONCE && alreadyTaken && !isRepeatable) {
                    continue;
                }

                boolean isOverdue = !displayDueDate.isAfter(now);
                result.add(new DueItem(template.getId(), template.getName(), displayDueDate, isOverdue));
            }
        }

        Set<UUID> seenTemplates = new HashSet<>();
        return result.stream()
                .sorted(Comparator
                        .comparing((DueItem i) -> i.dueAt())
                )
                .filter(i -> seenTemplates.add(i.templateId()))
                .toList();
    }

    private boolean checkIsDueOptimized(QuizAssignment assignment, Program program, Instant now, Instant dueDate) {
        int startOffset = Optional.ofNullable(assignment.getStartOffsetDay()).orElse(0);
        if (startOffset > 0 && program.getCurrentDay() < startOffset) {
            return false;
        }
        int interval = Optional.ofNullable(assignment.getEveryDays()).orElse(0);
        if (interval > 0) {
            return !dueDate.isAfter(now);
        }
        return true;
    }

    private Instant calculateDisplayDueDateOptimized(QuizAssignment assignment, Program program, QuizResult lastResult) {
        int startOffset = Optional.ofNullable(assignment.getStartOffsetDay()).orElse(0);
        int intervalDays = Optional.ofNullable(assignment.getEveryDays()).orElse(0);

        if (intervalDays > 0) {
            Instant lastDate = (lastResult != null) ? lastResult.getCreatedAt() : program.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            int everyDays = intervalDays > 0 ? intervalDays : 7;
            Instant candidate = lastDate.plus(Duration.ofDays(everyDays));

            if (startOffset > 0) {
                Instant earliest = program.getStartDate().plusDays(startOffset - 1).atStartOfDay(ZoneOffset.UTC).toInstant();
                return candidate.isBefore(earliest) ? earliest : candidate;
            }
            return candidate;
        }

        if (startOffset > 0) {
            return program.getStartDate().plusDays(startOffset - 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return program.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
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

        // Check Quiz Badges
        programRepository.findById(attempt.getProgramId())
                .ifPresent(badgeService::checkQuizProgress);

        // === LOGIC NGHIỆP VỤ ĐẶC BIỆT: XỬ LÝ SAU KHI NỘP QUIZ ===
        handlePostSubmissionActions(attempt);

        return new SubmitRes(attempt.getId(), totalScore, severity.name());
    }

    private void handlePostSubmissionActions(QuizAttempt attempt) {
        QuizAssignment assignment = quizAssignmentRepository
                .findActiveByProgramAndTemplate(attempt.getProgramId(), attempt.getTemplateId())
                .orElse(null);

        if (assignment == null) {
            return; // Không tìm thấy assignment, bỏ qua
        }
        if (!assignment.isActive()) {
            log.info("[QuizFlow] Assignment {} already inactive, skip post-submit handling.", assignment.getId());
            return;
        }

        if (assignment.getOrigin() == QuizAssignmentOrigin.STREAK_RECOVERY) {
            log.info("[QuizFlow] Handling STREAK_RECOVERY post-submission for program: {}", attempt.getProgramId());

            List<StreakBreak> breaks = streakBreakRepository
                    .findByProgramIdOrderByBrokenAtDesc(attempt.getProgramId());
            StreakBreak lastBreak = breaks.isEmpty() ? null : breaks.get(0);

            if (lastBreak != null) {
                var brokenStreak = streakRepository.findById(lastBreak.getStreakId()).orElse(null);
                if (brokenStreak != null && brokenStreak.getEndedAt() == null) {
                    log.info("[QuizFlow] Streak {} already restored, skip duplicate recovery.", brokenStreak.getId());
                } else {
                    log.info("[QuizFlow] Found last streak break {}. Calling StreakService to restore.", lastBreak.getId());
                    streakService.restoreStreak(lastBreak.getId());

                    Program program = programRepository.findById(attempt.getProgramId()).orElse(null);
                    if (program != null) {
                        program.setStreakRecoveryUsedCount(program.getStreakRecoveryUsedCount() + 1);
                        programRepository.save(program);
                    }
                }
            } else {
                log.warn("[QuizFlow] No streak break found for program {}. Cannot restore streak.", attempt.getProgramId());
            }

            assignment.setActive(false);
            quizAssignmentRepository.save(assignment);
        }

        // (Có thể thêm xử lý khác cho các loại quiz khác trong tương lai)
    }

    @Deprecated
    private void handlePostSubmissionActionsLegacy(QuizAttempt attempt) {
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
