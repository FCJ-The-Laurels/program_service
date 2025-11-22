-- ===========================
-- V28: add level column + seed plan templates (L1=30d, L2=45d, L3=60d)
-- Schema: program
-- ===========================

-- Cần cho gen_random_uuid()
create extension if not exists pgcrypto;

-- Bổ sung cột level cho plan_templates (cho cả DB cũ và DB mới)
alter table program.plan_templates
    add column if not exists level int;

-- Helper TABLE plan_steps đã có từ migration cũ, không tạo lại ở đây
-- (không dùng create table if not exists nữa)

-- Helper function (gọi trong DO, drop ở cuối)
create or replace function program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) returns void
language plpgsql as $fn$
begin
insert into program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes)
values (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm);
end
$fn$;

do $$
declare
l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
begin
insert into program.plan_templates(id, level, code, name, total_days)
values
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60)
    on conflict (id) do nothing;

-- ... toàn bộ phần perform program.add_step(...) giữ nguyên như bạn đã viết ...
end $$;

drop function if exists program.add_step(uuid,int,text,text,text,int);
