-- V22: keep columns, re-define the view safely
create or replace view program.v_recent_smoke_events as
select
    id,
    program_id,
    user_id,
    event_at as occurred_at,     -- giữ tên legacy
    kind,
    puffs,
    cigarettes,
    reason,
    repair_action,
    repaired,
    created_at,
    event_type,
    event_at,                    -- giữ nguyên cột event_at
    note,
    age(now(), event_at) as ago  -- nếu UI cần text thì dùng ::text
from program.smoke_events;
