package com.smokefree.program.domain.service.impl;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.QuizAssignment;
import com.smokefree.program.domain.model.QuizAssignmentOrigin;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.QuizAssignmentService;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAssignmentServiceImpl implements QuizAssignmentService {

    private final QuizAssignmentRepository quizAssignmentRepo;
    private final QuizTemplateRepository quizTemplateRepo;
    private final ProgramRepository programRepo; // Thêm dependency này

    @Override
    @Transactional
    public QuizAssignment assignRecoveryQuiz(UUID programId, String quizModuleCode, UUID streakBreakId) {
        log.info("[Recovery] Inside assignRecoveryQuiz service method. Finding template with code: {}", quizModuleCode);

        // Tìm QuizTemplate dựa trên code (moduleCode)
        var quizTemplate = quizTemplateRepo.findByCode(quizModuleCode)
                .orElseThrow(() -> new NotFoundException("Recovery Quiz Template not found with code: " + quizModuleCode));

        // Lấy thông tin program để biết ngày hiện tại
        Program program = programRepo.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        log.info("[Recovery] Found template {}. Assigning recovery quiz to program {} on day {}", 
                 quizTemplate.getId(), programId, program.getCurrentDay());

        QuizAssignment assignment = new QuizAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setProgramId(programId);
        assignment.setTemplateId(quizTemplate.getId());
        assignment.setScope(AssignmentScope.ONCE); // Quiz phục hồi chỉ làm 1 lần
        assignment.setOrigin(QuizAssignmentOrigin.STREAK_RECOVERY); // Đánh dấu nguồn gốc
        
        // SỬA LỖI: Gán quiz vào ngày hiện tại của chương trình
        assignment.setStartOffsetDay(program.getCurrentDay()); 
        assignment.setEveryDays(0);

        assignment.setActive(true);
        assignment.setCreatedAt(Instant.now());
        
        QuizAssignment savedAssignment = quizAssignmentRepo.save(assignment);
        log.info("[Recovery] Successfully saved new QuizAssignment with id: {}", savedAssignment.getId());
        return savedAssignment;
    }
}
