-- V20__program_content_modules.sql

create schema if not exists program;

-- Bảng module nội dung (JSONB + version + i18n)
create table if not exists program.content_modules (
                                                       id         uuid primary key default gen_random_uuid(),
    code       text not null,                 -- ví dụ: 'EDU_BENEFITS_24H'
    type       text not null,                 -- ví dụ: 'EDU_SLIDES_QUIZ'
    lang       text not null default 'vi',    -- 'vi' | 'en' | ...
    version    int  not null default 1,
    payload    jsonb not null,                -- slides/quiz/audio...
    updated_at timestamptz not null default now(),
    unique (code, lang, version)
    );
create index if not exists idx_content_modules_code_lang_ver
    on program.content_modules(code, lang, version desc);
create index if not exists idx_content_modules_code_lang
    on program.content_modules(code, lang);

-- Thêm module_code vào plan_steps (nếu chưa có)
alter table program.plan_steps
    add column if not exists module_code text;

-- Tuỳ chọn: với các step “EDU…/QUIZ…” gán module_code thay cho details
-- update program.plan_steps
-- set module_code = details, details = null
-- where details is not null and details ~ '^[A-Z0-9_]+$';
