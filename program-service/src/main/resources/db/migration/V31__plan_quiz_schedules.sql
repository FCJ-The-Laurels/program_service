-- V31: add plan_quiz_schedules + order_no for quiz_assignments

create table if not exists program.plan_quiz_schedules (
    id uuid primary key,
    plan_template_id uuid not null references program.plan_templates(id) on delete cascade,
    quiz_template_id uuid not null references program.quiz_templates(id) on delete cascade,
    start_offset_day int not null default 1,
    every_days int not null default 0,
    order_no int,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create index if not exists idx_plan_quiz_schedule_template
    on program.plan_quiz_schedules(plan_template_id, start_offset_day, order_no);

-- Add order_no to quiz_assignments to preserve schedule ordering
alter table program.quiz_assignments
    add column if not exists order_no int;
