-- ===========================================
-- R__helper_views_streaks.sql
-- Repeatable: các view hỗ trợ giám sát streak & sự kiện gần đây
-- ===========================================

CREATE OR REPLACE VIEW program.v_program_streaks AS
SELECT
    p.id,
    p.user_id,
    p.plan_days,
    p.status,
    p.current_streak_days,
    p.longest_streak_days,
    p.last_smoke_at,
    p.slip_count,
    (now() - p.last_smoke_at) AS since_last_smoke
FROM program.programs p;

CREATE OR REPLACE VIEW program.v_recent_smoke_events AS
SELECT
    se.*,
    (now() - se.occurred_at) AS ago
FROM program.smoke_events se
WHERE se.occurred_at >= now() - interval '14 days';
