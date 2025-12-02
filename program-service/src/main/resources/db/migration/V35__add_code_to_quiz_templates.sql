-- V35: Add 'code' column to quiz_templates table for recovery quizzes
ALTER TABLE program.quiz_templates
ADD COLUMN IF NOT EXISTS code VARCHAR(255);

-- Add a unique constraint to ensure codes are not duplicated
-- Note: This might fail if you have existing duplicate data (e.g., all NULLs).
-- For a new column, this should be safe.
ALTER TABLE program.quiz_templates
ADD CONSTRAINT uq_quiz_template_code UNIQUE (code);
