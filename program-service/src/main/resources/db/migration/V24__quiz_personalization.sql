-- ENUMs
do $$
begin
  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
     where n.nspname='program' and t.typname='quiz_template_owner_type'
  ) then
create type program.quiz_template_owner_type as enum ('SYSTEM','USER');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
     where n.nspname='program' and t.typname='quiz_assignment_origin'
  ) then
create type program.quiz_assignment_origin as enum
    ('AUTO_WEEKLY','MANUAL','COACH_CUSTOM');
end if;
end $$;

-- quiz_templates: thêm chủ sở hữu & nguồn gốc clone
alter table program.quiz_templates
    add column if not exists owner_type program.quiz_template_owner_type not null default 'SYSTEM',
    add column if not exists owner_id uuid,
    add column if not exists origin_template_id uuid
    references program.quiz_templates(id) on delete set null;

-- quiz_assignments: đánh dấu nguồn phát bài
alter table program.quiz_assignments
    add column if not exists origin program.quiz_assignment_origin not null default 'MANUAL';

create index if not exists idx_quiz_templates_owner on program.quiz_templates(owner_type, owner_id);
create index if not exists idx_quiz_assign_user_due on program.quiz_assignments(user_id, status, due_at);
