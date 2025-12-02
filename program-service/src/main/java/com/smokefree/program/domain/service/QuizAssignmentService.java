package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.QuizAssignment;
import java.util.UUID;

public interface QuizAssignmentService {
    QuizAssignment assignRecoveryQuiz(UUID programId, String quizModuleCode, UUID streakBreakId);
}
