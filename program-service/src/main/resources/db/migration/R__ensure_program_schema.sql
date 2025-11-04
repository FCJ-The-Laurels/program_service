-- R__ensure_program_schema.sql
-- Safe/idempotent: có thể chạy nhiều lần trên cùng DB.

-----------------------------
-- SCHEMA & OWNERSHIP
-----------------------------
do $$
begin
  if not exists (select 1 from pg_namespace where nspname = 'program') then
    execute 'create schema program';
end if;

begin
execute 'alter schema program owner to program_app_rw';
exception when insufficient_privilege then
    null;
end;
end $$;

-----------------------------
-- ENUMS (chỉ các enum còn dùng trong DB)
-----------------------------
do $$
begin
  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='quiz_template_status'
  ) then
create type program.quiz_template_status as enum ('DRAFT','PUBLISHED','ARCHIVED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='attempt_status'
  ) then
create type program.attempt_status as enum ('OPEN','SUBMITTED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='severity_level'
  ) then
create type program.severity_level as enum ('LOW','MODERATE','HIGH','VERY_HIGH');
end if;

  -- enum quiz_scope có thể tồn tại ở môi trường cũ; KHÔNG bắt buộc tạo mới.
  -- Nếu cần tương thích, bỏ comment khối dưới:
  -- if not exists (
  --   select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
  --   where n.nspname='program' and t.typname='quiz_scope'
  -- ) then
  --   create type program.quiz_scope as enum ('system','coach');
  -- end if;
end $$;

-----------------------------
-- programs (bảng gốc)
-- (Entity Program có startDate DATE NOT NULL và nhiều cột phụ)
-----------------------------
create table if not exists program.programs (
                                                id            uuid primary key,
                                                user_id       uuid not null,
                                                plan_days     int  not null,
                                                status        text not null,
                                                started_at    timestamptz,
                                                completed_at  timestamptz,
                                                created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    deleted_at    timestamptz
    );

-- Bổ sung cột còn thiếu cho programs
do $$
begin
  -- chatroom_id
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='chatroom_id'
  ) then
alter table program.programs add column chatroom_id uuid;
end if;

  -- coach_id
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='coach_id'
  ) then
alter table program.programs add column coach_id uuid;
end if;

  -- current_day (mặc định 1, NOT NULL)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='current_day'
  ) then
alter table program.programs add column current_day int;
alter table program.programs alter column current_day set default 1;
update program.programs set current_day = 1 where current_day is null;
alter table program.programs alter column current_day set not null;
end if;

  -- total_score
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='total_score'
  ) then
alter table program.programs add column total_score int;
end if;

  -- entitlement_tier_at_creation
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='entitlement_tier_at_creation'
  ) then
alter table program.programs add column entitlement_tier_at_creation text;
end if;

  -- trial_started_at
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='trial_started_at'
  ) then
alter table program.programs add column trial_started_at timestamptz;
end if;

  -- trial_end_expected
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='trial_end_expected'
  ) then
alter table program.programs add column trial_end_expected timestamptz;
end if;

  -- severity (enum)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='severity'
  ) then
alter table program.programs add column severity program.severity_level;
end if;

  -- start_date (LocalDate) + backfill từ started_at
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='start_date'
  ) then
alter table program.programs add column start_date date;
update program.programs
set start_date = (started_at at time zone 'UTC')::date
where start_date is null and started_at is not null;
-- nếu vẫn null (hàng cũ), set tạm ngày hôm nay UTC
update program.programs
set start_date = (now() at time zone 'UTC')::date
where start_date is null;
alter table program.programs alter column start_date set not null;
end if;
end $$;

-- FK chatroom_id nếu có bảng chatrooms
do $$
begin
  if exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='chatrooms'
  ) then
begin
alter table program.programs
    add constraint fk_program_chatroom
        foreign key (chatroom_id) references program.chatrooms(id);
exception when duplicate_object then null;
end;
end if;
end $$;

-----------------------------
-- quiz_templates & liên quan
-----------------------------
create table if not exists program.quiz_templates(
                                                     id            uuid primary key,
                                                     name          text not null,
                                                     version       int not null default 1,
                                                     status        program.quiz_template_status not null default 'DRAFT',
                                                     language_code text,
                                                     published_at  timestamptz,
                                                     archived_at   timestamptz,
                                                     scope         text not null default 'system',  -- dùng TEXT (không còn enum)
                                                     owner_id      uuid,
                                                     created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
    );

-- Thêm cột còn thiếu / chuyển kiểu scope enum -> text
do $$
declare
v_udt text;
begin
  -- owner_id (đề phòng bảng cũ chưa có)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='owner_id'
  ) then
alter table program.quiz_templates add column owner_id uuid;
end if;

  -- scope: nếu đang là enum program.quiz_scope thì chuyển sang text
select udt_name into v_udt
from information_schema.columns
where table_schema='program' and table_name='quiz_templates' and column_name='scope';

if v_udt = 'quiz_scope' then
    -- bỏ default trước để cắt phụ thuộc
begin
alter table program.quiz_templates alter column scope drop default;
exception when others then null; end;

alter table program.quiz_templates
alter column scope type text using scope::text;
end if;

  -- đặt default TEXT
begin
alter table program.quiz_templates alter column scope set default 'system';
exception when others then null; end;
end $$;

create table if not exists program.quiz_template_questions(
                                                              template_id uuid not null references program.quiz_templates(id) on delete cascade,
    question_no int not null,
    text        text not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    primary key (template_id, question_no)
    );

create table if not exists program.quiz_choice_labels(
                                                         template_id uuid not null,
                                                         question_no int not null,
                                                         score       int not null,
                                                         label       text not null,
                                                         primary key (template_id, question_no, score),
    foreign key (template_id, question_no)
    references program.quiz_template_questions(template_id, question_no)
    on delete cascade
    );

-- Unique constraint theo entity (@UniqueConstraint)
do $$
begin
  if not exists (
    select 1
    from pg_constraint c
    join pg_class     t on t.oid = c.conrelid
    join pg_namespace n on n.oid = t.relnamespace
    where n.nspname='program'
      and t.relname='quiz_templates'
      and c.conname='uq_quiz_template_name_scope_owner_version'
  ) then
alter table program.quiz_templates
    add constraint uq_quiz_template_name_scope_owner_version
        unique (name, scope, owner_id, version);
end if;
end $$;

-- Index theo entity (@Index scope, owner_id), chỉ tạo khi 2 cột đều tồn tại
do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='scope'
  ) and exists (
    select 1
    from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='owner_id'
  ) then
create index if not exists idx_quiz_template_scope_owner
    on program.quiz_templates(scope, owner_id);
end if;
end $$;

-----------------------------
-- quiz_assignments
-----------------------------
create table if not exists program.quiz_assignments(
                                                       id                 uuid primary key,
                                                       template_id        uuid not null references program.quiz_templates(id),
    program_id         uuid not null references program.programs(id) on delete cascade,
    assigned_by_user_id uuid,
    period_days        int,
    start_offset_day   int,
    use_latest_version boolean default true,
    active             boolean default true,
    created_at         timestamptz not null default now(),
    created_by         uuid,
    every_days         int not null default 5,
    scope              text not null default 'system'   -- TEXT (không còn enum)
    );

-- Bổ sung cột còn thiếu / chuyển kiểu scope enum -> text
do $$
declare v_udt text;
begin
  -- đảm bảo cột every_days
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='every_days'
  ) then
alter table program.quiz_assignments add column every_days int;
alter table program.quiz_assignments alter column every_days set default 5;
update program.quiz_assignments set every_days = 5 where every_days is null;
alter table program.quiz_assignments alter column every_days set not null;
end if;

  -- created_by
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='created_by'
  ) then
alter table program.quiz_assignments add column created_by uuid;
end if;

  -- scope: nếu chưa có thì thêm; nếu là enum thì chuyển type
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='scope'
  ) then
alter table program.quiz_assignments add column scope text;
alter table program.quiz_assignments alter column scope set default 'system';
update program.quiz_assignments set scope='system' where scope is null;
alter table program.quiz_assignments alter column scope set not null;
else
select udt_name into v_udt
from information_schema.columns
where table_schema='program' and table_name='quiz_assignments' and column_name='scope';

if v_udt = 'quiz_scope' then
begin
alter table program.quiz_assignments alter column scope drop default;
exception when others then null; end;

alter table program.quiz_assignments
alter column scope type text using scope::text;

begin
alter table program.quiz_assignments alter column scope set default 'system';
exception when others then null; end;
end if;
end if;
end $$;

create index if not exists idx_qas_program on program.quiz_assignments(program_id);

-----------------------------
-- quiz_attempts
-----------------------------
create table if not exists program.quiz_attempts(
                                                    id           uuid primary key,
                                                    program_id   uuid not null references program.programs(id) on delete cascade,
    template_id  uuid not null references program.quiz_templates(id),
    user_id      uuid not null,
    opened_at    timestamptz not null,
    submitted_at timestamptz,
    status       program.attempt_status not null default 'OPEN'
    );
create index if not exists idx_qatt_program_template
    on program.quiz_attempts(program_id, template_id, status);

-----------------------------
-- quiz_answers (PK (attempt_id, question_no))
-----------------------------
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='quiz_answers'
  ) then
create table program.quiz_answers(
                                     attempt_id  uuid not null,
                                     question_no int  not null,
                                     answer      integer,
                                     created_at  timestamptz not null default now(),
                                     primary key (attempt_id, question_no),
                                     constraint fk_qans_attempt foreign key (attempt_id)
                                         references program.quiz_attempts(id) on delete cascade
);
else
    -- ensure attempt_id
    if not exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers' and column_name='attempt_id'
    ) then
alter table program.quiz_answers add column attempt_id uuid;
end if;

    -- ensure PK
    if not exists (
      select 1
      from pg_constraint c
      join pg_class t on t.oid = c.conrelid
      join pg_namespace n on n.oid = t.relnamespace
      where n.nspname='program' and t.relname='quiz_answers'
        and c.contype='p'
    ) then
begin
alter table program.quiz_answers add primary key (attempt_id, question_no);
exception when duplicate_table then null; end;
end if;

    -- ensure FK
begin
alter table program.quiz_answers
    add constraint fk_qans_attempt foreign key (attempt_id)
        references program.quiz_attempts(id) on delete cascade;
exception when duplicate_object then null; end;

    -- ensure answer is integer
    if exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers'
        and column_name='answer' and data_type not in ('smallint','integer','bigint')
    ) then
begin
alter table program.quiz_answers
alter column answer type integer using nullif(trim(answer),'')::integer;
exception when others then
alter table program.quiz_answers rename column answer to answer_text;
alter table program.quiz_answers add column answer integer;
update program.quiz_answers
set answer = nullif(answer_text,'')::integer
where answer_text ~ '^\s*\d+\s*$';
end;
end if;

    -- ensure created_at
    if not exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers' and column_name='created_at'
    ) then
alter table program.quiz_answers add column created_at timestamptz default now();
end if;
end if;
end $$;

-----------------------------
-- quiz_results
-----------------------------
create table if not exists program.quiz_results(
                                                   id           uuid primary key,
                                                   program_id   uuid not null references program.programs(id) on delete cascade,
    template_id  uuid not null references program.quiz_templates(id),
    quiz_version int not null,
    total_score  int not null,
    severity     program.severity_level not null,
    created_at   timestamptz not null default now()
    );
create index if not exists idx_qres_program_template
    on program.quiz_results(program_id, template_id, created_at desc);

-----------------------------
-- TRY DROP enum quiz_scope nếu không còn dùng
-----------------------------
do $$
begin
  if exists (
    select 1 from pg_type t
    join pg_namespace n on n.oid = t.typnamespace
    where n.nspname='program' and t.typname='quiz_scope'
  ) then
    -- chỉ drop khi KHÔNG còn cột dùng udt_name='quiz_scope'
    if not exists (
      select 1
      from information_schema.columns
      where table_schema='program' and udt_name='quiz_scope'
    ) then
      execute 'drop type program.quiz_scope';
end if;
end if;
end $$;

-----------------------------
-- GRANTS (tùy quyền của user đang chạy)
-----------------------------
do $$
begin
begin
execute 'grant usage on schema program to program_app_rw';
execute 'grant select, insert, update, delete on all tables in schema program to program_app_rw';
execute 'grant usage, select, update on all sequences in schema program to program_app_rw';
execute 'alter default privileges in schema program grant select, insert, update, delete on tables to program_app_rw';
execute 'alter default privileges in schema program grant usage, select, update on sequences to program_app_rw';
exception when insufficient_privilege then
    null;
end;
end $$;
