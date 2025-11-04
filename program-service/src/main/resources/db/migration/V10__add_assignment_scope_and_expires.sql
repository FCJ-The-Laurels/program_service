-- =========================================================
-- V10__add_assignment_scope_and_expires.sql (FIXED)
-- Thêm enum scope + cột scope, expires_at cho các assignment
-- Idempotent & an toàn khi chạy nhiều lần
-- =========================================================

-- 1) Tạo enum program.assignment_scope nếu chưa có
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'program' AND t.typname = 'assignment_scope'
  ) THEN
    EXECUTE 'CREATE TYPE program.assignment_scope AS ENUM (''DAY'', ''WEEK'', ''PROGRAM'', ''CUSTOM'')';
END IF;
END
$$;

-- 2) Thêm cột cho các bảng assignment nếu bảng tồn tại
DO $$
BEGIN
  -- step_assignments
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'program' AND table_name = 'step_assignments'
  ) THEN
    EXECUTE 'ALTER TABLE program.step_assignments
             ADD COLUMN IF NOT EXISTS scope program.assignment_scope NOT NULL DEFAULT ''DAY''';
EXECUTE 'ALTER TABLE program.step_assignments
             ADD COLUMN IF NOT EXISTS expires_at timestamptz NULL';

EXECUTE 'COMMENT ON COLUMN program.step_assignments.scope IS ''Phạm vi hiệu lực: DAY/WEEK/PROGRAM/CUSTOM''';
EXECUTE 'COMMENT ON COLUMN program.step_assignments.expires_at IS ''Hạn hiệu lực (nếu có)''';

EXECUTE 'CREATE INDEX IF NOT EXISTS idx_step_assignments_expires_at
             ON program.step_assignments (expires_at)';
END IF;

  -- quiz_assignments (nếu có)
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'program' AND table_name = 'quiz_assignments'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_assignments
             ADD COLUMN IF NOT EXISTS scope program.assignment_scope NOT NULL DEFAULT ''DAY''';
EXECUTE 'ALTER TABLE program.quiz_assignments
             ADD COLUMN IF NOT EXISTS expires_at timestamptz NULL';

EXECUTE 'COMMENT ON COLUMN program.quiz_assignments.scope IS ''Phạm vi hiệu lực: DAY/WEEK/PROGRAM/CUSTOM''';
EXECUTE 'COMMENT ON COLUMN program.quiz_assignments.expires_at IS ''Hạn hiệu lực (nếu có)''';

EXECUTE 'CREATE INDEX IF NOT EXISTS idx_quiz_assignments_expires_at
             ON program.quiz_assignments (expires_at)';
END IF;

  -- (tuỳ dự án) nếu bạn có bảng assignment khác, lặp lại khối IF EXISTS tương tự
END
$$;
