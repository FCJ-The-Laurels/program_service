-- V13__add_event_at_to_smoke_events.sql
do $$
begin
  -- thêm cột event_at nếu chưa có
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='event_at'
  ) then
    execute 'alter table program.smoke_events add column event_at timestamptz';
    -- backfill: dùng created_at (nếu có) hoặc now()
execute 'update program.smoke_events set event_at = coalesce(created_at, now())';
execute 'alter table program.smoke_events alter column event_at set not null';
end if;

  -- index phục vụ truy vấn theo program + thời gian
  if not exists (
    select 1 from pg_indexes
    where schemaname='program' and indexname='idx_smoke_events_program_event_at'
  ) then
    execute 'create index idx_smoke_events_program_event_at
             on program.smoke_events (program_id, event_at desc)';
end if;
end $$;
