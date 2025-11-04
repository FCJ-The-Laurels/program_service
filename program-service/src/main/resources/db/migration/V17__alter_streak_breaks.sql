-- V17__alter_streak_breaks.sql

-- 1) Đổi tên cột broken_at -> broke_at (nếu V16 đang là broken_at)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='streak_breaks' AND column_name='broken_at'
  ) THEN
ALTER TABLE program.streak_breaks RENAME COLUMN broken_at TO broke_at;
END IF;
END$$;

-- 2) Bổ sung cột program_id, prev_streak_days
ALTER TABLE program.streak_breaks
    ADD COLUMN IF NOT EXISTS program_id uuid,
    ADD COLUMN IF NOT EXISTS prev_streak_days integer NOT NULL DEFAULT 0;

-- 3) Điền program_id từ smoke_events (nếu thiếu)
UPDATE program.streak_breaks sb
SET program_id = se.program_id
    FROM program.smoke_events se
WHERE sb.smoke_event_id = se.id AND sb.program_id IS NULL;

-- 4) Ràng buộc NOT NULL theo entity mới
ALTER TABLE program.streak_breaks
    ALTER COLUMN program_id SET NOT NULL,
ALTER COLUMN smoke_event_id SET NOT NULL;

-- 5) (tuỳ chọn) default cho created_at
ALTER TABLE program.streak_breaks
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'utc');
