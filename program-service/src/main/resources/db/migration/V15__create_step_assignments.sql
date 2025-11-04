-- V15__create_step_assignments.sql
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='step_assignments'
  ) then
    execute $SQL$
create table program.step_assignments (
                                          id            uuid primary key,
                                          program_id    uuid not null references program.programs(id) on delete cascade,

    -- thông tin step
                                          step_no       int  not null,                     -- thứ tự/bậc của step trong chương trình (1..N)
                                          planned_day   int  not null,                     -- ngày N trong plan (ví dụ: 1..30)
                                          status        varchar(32) not null default 'PENDING', -- map Enum STRING
                                          scheduled_at  timestamptz,                       -- thời điểm dự kiến
                                          completed_at  timestamptz,                       -- hoàn tất lúc
                                          note          text,

    -- audit
                                          created_at    timestamptz not null default now(),
                                          created_by    uuid,
                                          updated_at    timestamptz not null default now()
);
create index on program.step_assignments(program_id);
create unique index ux_step_assignments_program_step
    on program.step_assignments(program_id, step_no);
$SQL$;
end if;

  -- trigger update updated_at (tuỳ thích)
  if not exists (
    select 1 from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'program' and p.proname = 'touch_updated_at'
  ) then
    execute $SQL$
      create or replace function program.touch_updated_at()
      returns trigger language plpgsql as $FN$
begin
        new.updated_at = now();
return new;
end $FN$;
    $SQL$;
end if;

  if not exists (
    select 1 from pg_trigger t
    join pg_class c on c.oid = t.tgrelid
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname='program' and c.relname='step_assignments' and t.tgname='trg_step_assignments_touch_updated_at'
  ) then
    execute $SQL$
create trigger trg_step_assignments_touch_updated_at
    before update on program.step_assignments
    for each row execute function program.touch_updated_at();
$SQL$;
end if;
end $$;
