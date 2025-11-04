-- ===========================================
-- V8__add_streak_columns_to_programs.sql
-- Thêm cột theo dõi streak cho program.programs
-- Idempotent (AN TOÀN chạy nhiều lần)
-- ===========================================

-- (Giả sử schema program đã được tạo bởi R__ensure_program_schema.sql)

ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS current_streak_days INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS longest_streak_days INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_smoke_at       TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS slip_count          INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN program.programs.current_streak_days IS 'Số ngày sạch liên tiếp tính đến hôm nay';
COMMENT ON COLUMN program.programs.longest_streak_days IS 'Chuỗi sạch dài nhất đã đạt';
COMMENT ON COLUMN program.programs.last_smoke_at       IS 'Thời điểm gần nhất có hút (slip/relapse)';
COMMENT ON COLUMN program.programs.slip_count          IS 'Tổng số lần slip (không bắt buộc tính relapse)';
