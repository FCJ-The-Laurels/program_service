-- V14__add_note_to_smoke_events.sql
do $$
begin
  -- Thêm cột note nếu chưa có
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='note'
  ) then
    execute 'alter table program.smoke_events add column note text';
    -- nếu bạn muốn không null thì dùng 2 dòng dưới (tuỳ entity có nullable=false hay không)
    -- execute '' || 'update program.smoke_events set note = '''''' where note is null';
    -- execute '' || 'alter table program.smoke_events alter column note set not null';
end if;
end $$;
