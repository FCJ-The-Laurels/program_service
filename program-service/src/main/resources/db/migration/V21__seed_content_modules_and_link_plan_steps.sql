-- V21__seed_content_modules_and_link_plan_steps.sql
-- Mục tiêu:
-- - Bảng content_modules (JSONB, version, lang)
-- - Bổ sung module_code cho plan_steps
-- - Seed module + link vào các bước của L1
-- - Sửa thiếu 1 step ở L1 ngày 30 (slot 16:30)
-- - Verify

BEGIN;

CREATE SCHEMA IF NOT EXISTS program;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Bảng content_modules
CREATE TABLE IF NOT EXISTS program.content_modules (
                                                       id         uuid PRIMARY KEY,
                                                       code       text NOT NULL,
                                                       type       text NOT NULL,
                                                       lang       text NOT NULL DEFAULT 'vi',
                                                       version    integer NOT NULL DEFAULT 1,
                                                       payload    jsonb NOT NULL,
                                                       updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_modules UNIQUE (code, lang, version)
    );

CREATE INDEX IF NOT EXISTS idx_cm_code_lang_ver
    ON program.content_modules(code, lang, version);

-- 2) Bổ sung cột module_code cho plan_steps
ALTER TABLE program.plan_steps
    ADD COLUMN IF NOT EXISTS module_code text;

-- 3) Helper upsert function cho content_modules (idempotent)
CREATE OR REPLACE FUNCTION program.put_module(
  p_code text, p_type text, p_lang text, p_version int, p_payload jsonb
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE rid uuid;
BEGIN
SELECT id INTO rid
FROM program.content_modules
WHERE code=p_code AND lang=p_lang AND version=p_version;

IF rid IS NOT NULL THEN
    -- giữ nguyên để idempotent
    RETURN rid;
END IF;

INSERT INTO program.content_modules(id, code, type, lang, version, payload, updated_at)
VALUES (gen_random_uuid(), p_code, p_type, p_lang, p_version, p_payload, now())
    RETURNING id INTO rid;

RETURN rid;
END $$;

-- 4) Seed các module (VI). Có thể mở rộng EN nếu cần
-- 4.1 Urge Log 4D (TASK)
SELECT program.put_module(
               'TASK_URGELOG_4D','TASK','vi',1,
               '{
                 "title":"Urge Log + 4D",
                 "howto":["Nhận diện cơn thèm (0-10)","Delay 3 phút","Deep breathing","Drink/Do: uống nước/đi bộ ngắn"],
                 "timerSeconds":180,
                 "fields":["trigger","intensity","note"]
               }'::jsonb
       );

-- 4.2 Lợi ích 24h (EDU_SLIDES) v1 & v2
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','vi',1,
               '{
                 "title":"Lợi ích sau 24 giờ bỏ thuốc",
                 "slides":[
                   {"h":"Tim mạch","bullets":["Huyết áp ổn định hơn","Nhịp tim dần bình thường"]},
                   {"h":"Hô hấp","bullets":["CO giảm về mức bình thường","Oxy trong máu tăng lên"]}
                 ],
                 "quiz":[{"q":"Sau 24h, CO thay đổi thế nào?","options":["Tăng","Giảm về bình thường"],"answer":1}]
               }'::jsonb
       );
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','vi',2,
               '{
                 "title":"Lợi ích sau 24 giờ (v2)",
                 "slides":[
                   {"h":"Tim mạch","bullets":["Huyết áp giảm","Giảm co thắt mạch"]},
                   {"h":"Hô hấp","bullets":["CO giảm rõ","Oxy tăng"]}
                 ],
                 "cta":{"text":"Tiếp tục lộ trình","deeplink":"app://plan/next"}
               }'::jsonb
       );

-- 4.3 Body scan 5 phút (AUDIO)
SELECT program.put_module(
               'MINDSL_BODYSCAN_5M','AUDIO','vi',1,
               '{
                 "title":"Mindfulness body-scan 5 phút",
                 "audioUrl":"https://cdn.example.com/audio/bodyscan-5m.mp3",
                 "transcript":"Hướng dẫn body-scan từ đầu đến chân",
                 "durationSec":300
               }'::jsonb
       );

-- 4.4 Social scripts (EDU_TEMPLATES)
SELECT program.put_module(
               'EDU_SOCIAL_SCRIPTS','EDU_TEMPLATES','vi',1,
               '{
                 "title":"Kịch bản xã hội",
                 "scenes":[
                   {"name":"Cà phê","lines":["Cảm ơn, mình đang cai thuốc","Cho mình cốc nước nhé"]},
                   {"name":"Đồng nghiệp","lines":["Mình nghỉ thuốc rồi","Ra ngoài hít thở chút thay vì hút"]}
                 ]
               }'::jsonb
       );

-- 4.5 Pomodoro 25–5 (TASK)
SELECT program.put_module(
               'TASK_POMODORO_25_5','TASK','vi',1,
               '{
                 "title":"Pomodoro 25–5",
                 "workMin":25,"breakMin":5,
                 "tips":["Rời chỗ ngồi khi giải lao","Không kèm điếu thuốc"],
                 "timer":true
               }'::jsonb
       );

-- 4.6 Bộ giảm stress 10 phút (PACK)
SELECT program.put_module(
               'PACK_STRESS_10M','PACK','vi',1,
               '{
                 "title":"Bộ giảm stress 10 phút",
                 "items":[
                   {"type":"breath","label":"Thở 4-7-8 (2p)"},
                   {"type":"stretch","label":"Giãn cơ cổ vai (3p)"},
                   {"type":"walk","label":"Đi bộ chậm (5p)"}
                 ]
               }'::jsonb
       );

-- 4.7 5 thói quen ngủ (EDU_LIST)
SELECT program.put_module(
               'EDU_SLEEP_5HABITS','EDU_LIST','vi',1,
               '{
                 "title":"5 thói quen ngủ",
                 "items":[
                   "Giảm màn hình 1 giờ trước khi ngủ",
                   "Tránh cà phê sau 15h",
                   "Phòng mát/thoáng",
                   "Giữ giờ ngủ cố định",
                   "Thư giãn ngắn trước khi ngủ"
                 ]
               }'::jsonb
       );

-- 4.8 Đi bộ/giãn cơ 10 phút (TASK)
SELECT program.put_module(
               'TASK_WALK_10M','TASK','vi',1,
               '{
                 "title":"Vận động nhẹ 10 phút",
                 "actions":["Đi bộ 1000 bước hoặc giãn cơ toàn thân"],
                 "timerSeconds":600
               }'::jsonb
       );

-- 4.9 Từ chối khéo (EDU_TEMPLATES)
SELECT program.put_module(
               'SOCIAL_SAY_NO','EDU_TEMPLATES','vi',1,
               '{
                 "title":"Tập nói \"không\"",
                 "phrases":["Mình nghỉ thuốc rồi, cảm ơn","Mình ra hít thở chút nhé"]
               }'::jsonb
       );

-- 4.10 Tình huống rủi ro cao (EDU_PLANNER)
SELECT program.put_module(
               'HIGH_RISK_SCENARIOS','EDU_PLANNER','vi',1,
               '{
                 "title":"Tình huống rủi ro cao",
                 "prompts":["Sau bữa ăn","Căng thẳng công việc","Khi buồn chán"],
                 "planField":"Phương án ứng phó"
               }'::jsonb
       );

-- 4.11 Câu chuyện thành công + quiz (EDU_STORY)
SELECT program.put_module(
               'STORY_SUCCESS_MINIQUIZ','EDU_STORY','vi',1,
               '{
                 "title":"Câu chuyện thành công",
                 "story":"Sau 3 tuần, A hết thèm khi uống nước và đi bộ ngắn.",
                 "quiz":[{"q":"Chiến lược hiệu quả nhất của A?","options":["Ngủ nhiều hơn","Uống nước + đi bộ"],"answer":1}]
               }'::jsonb
       );

-- 4.12 Kế hoạch phòng tái sử dụng (EDU_PLANNER)
SELECT program.put_module(
               'RELAPSE_PREVENTION_PLAN','EDU_PLANNER','vi',1,
               '{
                 "title":"Kế hoạch phòng tái sử dụng",
                 "traps":["Tiệc với bạn","Căng thẳng","Một điếu không sao"],
                 "actions":["Mang nước","Gọi buddy","Thở 3 phút + rời chỗ"]
               }'::jsonb
       );

-- (tùy chọn) vài bản EN mẫu
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','en',1,
               '{"title":"24h Benefits","slides":[{"h":"Cardio","bullets":["BP stabilizes"]}]}'::jsonb
       );


-- 5) Gắn module_code vào step của L1
DO $$
DECLARE l1 uuid := '11111111-1111-1111-1111-111111111111';
    i int;
BEGIN
  -- Ngày 1
UPDATE program.plan_steps SET module_code='TASK_URGELOG_4D'
WHERE template_id=l1 AND day_no=1 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_BENEFITS_24H'
WHERE template_id=l1 AND day_no=1 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 3
UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS'
WHERE template_id=l1 AND day_no=3 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='MINDSL_BODYSCAN_5M'
WHERE template_id=l1 AND day_no=3 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 4
UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5'
WHERE template_id=l1 AND day_no=4 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 8..14
UPDATE program.plan_steps SET module_code='PACK_STRESS_10M'
WHERE template_id=l1 AND day_no=8 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_SLEEP_5HABITS'
WHERE template_id=l1 AND day_no=9 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='TASK_WALK_10M'
WHERE template_id=l1 AND day_no=10 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS'
WHERE template_id=l1 AND day_no=11 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5'
WHERE template_id=l1 AND day_no=12 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='SOCIAL_SAY_NO'
WHERE template_id=l1 AND day_no=13 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 15..21: 12:30 = rủi ro cao
FOR i IN 15..21 LOOP
UPDATE program.plan_steps SET module_code='HIGH_RISK_SCENARIOS'
WHERE template_id=l1 AND day_no=i AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
END LOOP;

  -- Ngày 22..29: 16:30 = story + mini-quiz
FOR i IN 22..29 LOOP
UPDATE program.plan_steps SET module_code='STORY_SUCCESS_MINIQUIZ'
WHERE template_id=l1 AND day_no=i AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
END LOOP;

  -- Ngày 30: 12:30 = kế hoạch phòng tái
UPDATE program.plan_steps SET module_code='RELAPSE_PREVENTION_PLAN'
WHERE template_id=l1 AND day_no=30 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');

-- Bước bị thiếu ở L1 ngày 30 lúc 16:30 (đảm bảo đủ 120 bước)
INSERT INTO program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes, created_at, module_code)
SELECT gen_random_uuid(), l1, 30, '16:30'::time,
    'Lễ huy hiệu + chia sẻ thành tựu', 'Nhìn lại 3 bài học rút ra', 10, now(), 'STORY_SUCCESS_MINIQUIZ'
    WHERE NOT EXISTS (
    SELECT 1 FROM program.plan_steps WHERE template_id=l1 AND day_no=30 AND slot='16:30'::time
  );
END $$;

-- 6) Verify nhanh
-- 6.1 Đủ 4 bước mỗi ngày cho L1?
WITH t AS (
    SELECT total_days FROM program.plan_templates WHERE id='11111111-1111-1111-1111-111111111111'
)
SELECT 'L1_30D' AS code,
       (SELECT total_days FROM t) AS total_days,
       (SELECT total_days*4 FROM t) AS expected_steps_4perday,
       (SELECT COUNT(*) FROM program.plan_steps WHERE template_id='11111111-1111-1111-1111-111111111111') AS actual_steps,
       ((SELECT COUNT(*) FROM program.plan_steps WHERE template_id='11111111-1111-1111-1111-111111111111')
           = (SELECT total_days*4 FROM t)) AS ok;

-- 6.2 Bước nào còn chưa có module_code?
SELECT day_no, slot, title
FROM program.plan_steps
WHERE template_id='11111111-1111-1111-1111-111111111111' AND (module_code IS NULL OR module_code='')
ORDER BY day_no, slot;

-- 6.3 Thống kê theo module_code
SELECT module_code, COUNT(*) cnt
FROM program.plan_steps
WHERE template_id='11111111-1111-1111-1111-111111111111'
GROUP BY module_code
ORDER BY cnt DESC NULLS LAST;

COMMIT;

-- (Tuỳ chọn) Dọn helper
-- DROP FUNCTION IF EXISTS program.put_module(text,text,text,int,jsonb);
