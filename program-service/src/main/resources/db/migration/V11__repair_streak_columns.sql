-- Idempotent: an toàn chạy nhiều lần.

-- Nếu trước đó lỡ đặt tên ngược (best_streak / current_streak) thì đổi về đúng chuẩn
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='best_streak'
  ) then
    execute 'alter table program.programs rename column best_streak to streak_best';
end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='current_streak'
  ) then
    execute 'alter table program.programs rename column current_streak to streak_current';
end if;
end $$;

-- Đảm bảo các cột streak tồn tại
alter table program.programs
    add column if not exists streak_current int not null default 0,
    add column if not exists streak_best    int not null default 0,
    add column if not exists last_smoke_at timestamptz null,
    add column if not exists streak_frozen_until timestamptz null;

-- Backfill: best >= current
update program.programs
set streak_best = greatest(coalesce(streak_best, 0), coalesce(streak_current, 0))
where coalesce(streak_current, 0) > 0;
