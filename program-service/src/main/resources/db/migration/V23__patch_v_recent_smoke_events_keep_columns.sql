-- V23__patch_v_recent_smoke_events_keep_columns.sql
create or replace view program.v_recent_smoke_events as
select
    id,
    program_id,
    user_id,
    -- Giữ tên legacy 'occurred_at' nhưng lấy từ event_at chuẩn mới
    event_at as occurred_at,
    kind,
    puffs,
    cigarettes,
    reason,
    repair_action,
    repaired,
    created_at,
    event_type,
    event_at,                 -- giữ lại cột này để không "drop column"
    note,
    age(now(), event_at) as ago  -- nếu trước đây 'ago' là interval
from program.smoke_events;
