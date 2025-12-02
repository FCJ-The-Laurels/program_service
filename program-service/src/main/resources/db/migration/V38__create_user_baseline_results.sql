-- V38: Table lưu kết quả quiz onboarding (baseline) theo user

create table if not exists program.user_baseline_results (
    id uuid primary key,
    user_id uuid not null,
    quiz_template_id uuid not null,
    total_score integer not null,
    severity program.severity_level not null,
    created_at timestamptz not null default now()
);

create unique index if not exists uq_user_baseline_user on program.user_baseline_results(user_id);
create index if not exists idx_user_baseline_template on program.user_baseline_results(quiz_template_id);
