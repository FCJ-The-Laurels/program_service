-- Thêm cột note cho bảng streak_breaks để khớp với entity
ALTER TABLE program.streak_breaks
    ADD COLUMN IF NOT EXISTS note text;

-- (tuỳ chọn) đảm bảo created_at có default UTC
ALTER TABLE program.streak_breaks
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'utc');
