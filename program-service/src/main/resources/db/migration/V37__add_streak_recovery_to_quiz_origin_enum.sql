-- V37: Add STREAK_RECOVERY value to the quiz_assignment_origin enum type
ALTER TYPE program.quiz_assignment_origin ADD VALUE IF NOT EXISTS 'STREAK_RECOVERY';
