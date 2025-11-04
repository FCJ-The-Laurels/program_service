package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.QuizAssignment;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.service.quiz.QuizAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizAssignmentServiceImpl implements QuizAssignmentService {

    private final QuizAssignmentRepository assignmentRepo;
    private final ProgramRepository programRepo;

    @Override
    public void assignToPrograms(UUID templateId,
                                 List<UUID> programIds,
                                 int everyDays,
                                 UUID actorId,
                                 String scope) {
        if (programIds == null || programIds.isEmpty()) return;

        // 1) "coach" đang được dùng như QUYỀN tác giả -> tách riêng
        boolean coachMode = scope != null && "coach".equalsIgnoreCase(scope);

        // 2) scope gán xuống DB phải là ENUM; nếu không parse được thì mặc định DAY
        AssignmentScope assignmentScope = safeParseAssignmentScope(scope);

        List<QuizAssignment> batch = new ArrayList<>();
        for (UUID pid : programIds) {
            // quyền coach: xác minh coach là chủ program
            if (coachMode) {
                if (!programRepo.existsByIdAndCoachId(pid, actorId)) continue;
            }
            // tránh trùng assignment
            if (assignmentRepo.existsByTemplateIdAndProgramId(templateId, pid)) continue;

            QuizAssignment a = new QuizAssignment();
            a.setId(UUID.randomUUID());
            a.setTemplateId(templateId);
            a.setProgramId(pid);
            a.setEveryDays(everyDays);
            a.setCreatedAt(Instant.now());
            a.setCreatedBy(actorId);

            // ✅ gán ENUM thay vì String
            a.setScope(assignmentScope);

            batch.add(a);
        }
        assignmentRepo.saveAll(batch);
    }

    private AssignmentScope safeParseAssignmentScope(String s) {
        if (s == null) return AssignmentScope.DAY;
        try {
            return AssignmentScope.valueOf(s.trim().toUpperCase()); // DAY/WEEK/PROGRAM/CUSTOM
        } catch (IllegalArgumentException ex) {
            // nếu người gọi truyền "coach" hoặc rác -> fallback
            return AssignmentScope.DAY;
        }
    }

    @Override
    public List<QuizAssignment> listAssignmentsByProgram(UUID programId) {
        return assignmentRepo.findByProgramId(programId);
    }
}
