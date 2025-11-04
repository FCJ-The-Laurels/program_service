-- ===========================================
-- V9__create_smoke_events.sql  (FIXED)
-- Log sự kiện SLIP/RELAPSE + enum type
-- Idempotent, an toàn chạy nhiều lần
-- ===========================================

-- 1) Tạo enum kiểu sự kiện nếu chưa có
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'program' AND t.typname = 'smoke_event_kind'
  ) THEN
    EXECUTE 'CREATE TYPE program.smoke_event_kind AS ENUM (''SLIP'', ''RELAPSE'')';
END IF;
END
$$;

-- 2) Tạo bảng nếu chưa có (id chưa set default để tránh phụ thuộc pgcrypto)
CREATE TABLE IF NOT EXISTS program.smoke_events (
                                                    id            UUID PRIMARY KEY,
                                                    program_id    UUID NOT NULL REFERENCES program.programs(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    kind          program.smoke_event_kind NOT NULL,
    puffs         INT NULL,        -- số hơi (tuỳ chọn)
    cigarettes    INT NULL,        -- số điếu (tuỳ chọn)
    reason        TEXT NULL,       -- lý do (map từ câu hỏi)
    repair_action TEXT NULL,       -- hành động sửa (map từ câu hỏi)
    repaired      BOOLEAN NULL,    -- đã làm hành động sửa ngay?
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
    );

COMMENT ON TABLE  program.smoke_events          IS 'Ghi lại các sự kiện SLIP/RELAPSE để điều chỉnh streak và lộ trình';
COMMENT ON COLUMN program.smoke_events.kind     IS 'SLIP=lỡ 1–2 hơi/điếu rồi quay lại; RELAPSE=tái hút có hệ thống';
COMMENT ON COLUMN program.smoke_events.repaired IS 'User đã thực hiện hành động sửa ngay sau slip?';

-- 3) Nếu có hàm gen_random_uuid() thì set default cho id (không cần quyền SUPERUSER)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'gen_random_uuid') THEN
    EXECUTE 'ALTER TABLE program.smoke_events ALTER COLUMN id SET DEFAULT gen_random_uuid()';
END IF;
END
$$;

-- 4) Indexes phục vụ truy vấn
CREATE INDEX IF NOT EXISTS idx_smoke_events_program_time
    ON program.smoke_events (program_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_smoke_events_user_time
    ON program.smoke_events (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_smoke_events_kind
    ON program.smoke_events (kind);
