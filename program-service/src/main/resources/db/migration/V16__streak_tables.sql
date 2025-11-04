-- V16__streak_tables.sql
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='streaks'
  ) then
    execute $SQL$
create table program.streaks (
                                 id uuid primary key,
                                 program_id uuid not null references program.programs(id) on delete cascade,
                                 started_at timestamptz not null,
                                 ended_at   timestamptz,
                                 length_days int,
                                 created_at timestamptz not null default now()
);
create index on program.streaks(program_id, started_at desc);
$SQL$;
end if;

  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='streak_breaks'
  ) then
    execute $SQL$
create table program.streak_breaks (
                                       id uuid primary key,
                                       streak_id uuid not null references program.streaks(id) on delete cascade,
                                       smoke_event_id uuid references program.smoke_events(id),
                                       broken_at timestamptz not null,
                                       reason text,
                                       created_at timestamptz not null default now()
);
create index on program.streak_breaks(streak_id, broken_at desc);
$SQL$;
end if;
end $$;
