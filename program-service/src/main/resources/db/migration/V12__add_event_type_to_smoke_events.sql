-- V12__add_event_type_to_smoke_events.sql
-- Tạo ENUM cho loại sự kiện hút/khôi phục (nếu chưa có)
do $$
begin
  if not exists (
    select 1
    from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='smoke_event_type'
  ) then
    execute $ct$
create type program.smoke_event_type as enum
    ('SMOKE','RECOVERY_START','RECOVERY_SUCCESS','RECOVERY_FAIL')
    $ct$;
end if;
end $$;

-- Thêm cột event_type nếu chưa có và backfill
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='event_type'
  ) then
    execute 'alter table program.smoke_events add column event_type program.smoke_event_type';
execute 'update program.smoke_events set event_type = ''SMOKE'' where event_type is null';
execute 'alter table program.smoke_events alter column event_type set not null';
execute 'alter table program.smoke_events alter column event_type set default ''SMOKE''';
end if;
end $$;
