-- V29: seed plan_steps cho 3 level (L1=30d, L2=45d, L3=60d)

create extension if not exists pgcrypto;

-- Helper function: thêm 1 step vào plan_steps
create or replace function program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) returns void
language plpgsql as $fn$
begin
insert into program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes)
values (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm);
end
$fn$;

do $seed$
declare
l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
begin
  -- Đảm bảo 3 template tồn tại
insert into program.plan_templates(id, level, code, name, total_days)
values
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60)
    on conflict (id) do nothing;

-- =========================
-- L1 — THỨC TỈNH (30 ngày)
-- =========================

-- Ngày 1
perform program.add_step(l1,1,'07:30','Check-in sáng + chọn mục tiêu 7 ngày',
      'Kéo thang thèm/stress; chọn giấc ngủ; gợi ý: Thở 3’, Uống nước, Đi bộ 3’',10);
  perform program.add_step(l1,1,'12:30','Học 4D + tạo Urge Log thử',
      'Chọn nguyên nhân & mức độ; chạy Timer 3’ để thực hành 4D',7);
  perform program.add_step(l1,1,'16:30','Lợi ích sau 24h bỏ thuốc (mini-slide + quiz)',
      '3 slide + 3 câu hỏi',5);
  perform program.add_step(l1,1,'21:00','Nhật ký 2 câu + chọn Mantra',
      'Viết 1–2 câu; chọn câu động lực (có giọng đọc)',12);

  -- Ngày 2
  perform program.add_step(l1,2,'07:30','Checklist dọn môi trường + nhắn người thân',
      'Bỏ bật lửa/tàn thuốc; giặt áo ám mùi; nhắn người thân',12);
  perform program.add_step(l1,2,'12:30','Urge Log + Timer 3’ (4D)',
      'Chọn nguyên nhân/mức độ; vượt cơn 3’',8);
  perform program.add_step(l1,2,'16:30','CBT nhanh: thay câu nghĩ “một điếu không sao”',
      'Điền A–B–C và câu thay thế thực tế hơn',12);
  perform program.add_step(l1,2,'21:00','Tổng kết + Mantra','Viết ngắn; đặt câu cho ngày mai',12);

  -- Ngày 3
  perform program.add_step(l1,3,'07:30','Check-in + thở hướng dẫn 5’','Nếu thèm ≥7, nghe thở 5’',12);
  perform program.add_step(l1,3,'12:30','Kịch bản xã hội','Cà phê/đồng nghiệp/một mình – chọn câu từ chối',10);
  perform program.add_step(l1,3,'16:30','Mindfulness body-scan 5’','Nghe hướng dẫn, ghi 1 câu cảm nhận',12);
  perform program.add_step(l1,3,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 4
  perform program.add_step(l1,4,'07:30','Heatmap giờ hay thèm + áp lịch nhắc','Chọn 2–3 khung giờ nhắc',10);
  perform program.add_step(l1,4,'12:30','Chọn vật thay thế tay–miệng 24h','Kẹo the/tăm/đồ bóp tay',8);
  perform program.add_step(l1,4,'16:30','1 phiên Pomodoro 25–5 không thuốc',null,12);
  perform program.add_step(l1,4,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 5
  perform program.add_step(l1,5,'07:30','Tuyên ngôn “mình là người không hút” + 1 hành động',
      'Ví dụ: 2 chai nước / đi bộ 10’',10);
  perform program.add_step(l1,5,'12:30','Urge Log + mini-quiz tim mạch','2 câu',10);
  perform program.add_step(l1,5,'16:30','Tập nói “không” (nghe mẫu + chọn câu)',null,12);
  perform program.add_step(l1,5,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 6
  perform program.add_step(l1,6,'07:30','Slip? Flow phục hồi','Chọn lý do và 1 hành động sửa; không reset toàn bộ streak',12);
  perform program.add_step(l1,6,'12:30','Kế hoạch cuối tuần không khói',null,10);
  perform program.add_step(l1,6,'16:30','Mindfulness 5’ + động viên',null,12);
  perform program.add_step(l1,6,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 7
  perform program.add_step(l1,7,'07:30','Khảo sát 7 ngày + Check-in',null,12);
  perform program.add_step(l1,7,'12:30','Rút kinh nghiệm: Top-3 chiến lược',null,10);
  perform program.add_step(l1,7,'16:30','Chọn phần thưởng nhỏ',null,10);
  perform program.add_step(l1,7,'21:00','Kế hoạch tuần sau','Chọn trọng tâm (ngủ/xã hội/Pomodoro...)',12);

  -- Ngày 8..14
  perform program.add_step(l1,8,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,8,'12:30','Urge Log',null,8);
  perform program.add_step(l1,8,'16:30','Bộ giảm stress 10’','Thở, giãn cơ, đi bộ ngắn',12);
  perform program.add_step(l1,8,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,9,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,9,'12:30','Urge Log',null,8);
  perform program.add_step(l1,9,'16:30','5 thói quen ngủ','Giảm màn hình; tránh cafe sau 15h; phòng mát/thoáng',10);
  perform program.add_step(l1,9,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,10,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,10,'12:30','Urge Log',null,8);
  perform program.add_step(l1,10,'16:30','Đi bộ/giãn cơ 10’','Đặt timer; hướng 1000 bước',12);
  perform program.add_step(l1,10,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,11,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,11,'12:30','Kịch bản xã hội','Chọn tình huống + câu nói',12);
  perform program.add_step(l1,11,'16:30','Urge Log',null,8);
  perform program.add_step(l1,11,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,12,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,12,'12:30','Urge Log',null,8);
  perform program.add_step(l1,12,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l1,12,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,13,'07:30','Khẳng định bản sắc + 1 hành động',null,10);
  perform program.add_step(l1,13,'12:30','Urge Log',null,8);
  perform program.add_step(l1,13,'16:30','Từ chối khéo','Nghe mẫu + chọn câu',12);
  perform program.add_step(l1,13,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,14,'07:30','Check-in tổng tuần',null,10);
  perform program.add_step(l1,14,'12:30','Top-3 chiến lược hiệu quả',null,10);
  perform program.add_step(l1,14,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l1,14,'21:00','Chọn trọng tâm tuần tới',null,12);

  -- Ngày 15..21 (loop)
for i in 15..21 loop
      perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l1,i,'12:30','Tình huống rủi ro cao','Chọn kịch bản khó + phương án ứng phó',12);
      perform program.add_step(l1,i,'16:30','Theo dõi thói quen thay thế','Đánh dấu uống nước/đi bộ/giãn cơ',12);
      perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 22..29 (loop)
for i in 22..29 loop
      perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l1,i,'12:30','Cập nhật tiền tiết kiệm','Nhập số tiền không chi cho thuốc hôm nay',8);
      perform program.add_step(l1,i,'16:30','Câu chuyện thành công + mini-quiz','Đọc 1 câu chuyện; trả lời 2 câu',10);
      perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 30
  perform program.add_step(l1,30,'07:30','Tổng kết tiến trình 30 ngày',
      'Xem huy hiệu/điểm/tiền tiết kiệm/heatmap',12);
  perform program.add_step(l1,30,'12:30','Cập nhật kế hoạch phòng tái','3 bẫy lớn + 3 hành động sẵn',12);
  perform program.add_step(l1,30,'21:00','Chọn lộ trình tiếp theo','Duy trì hoặc chuyển Level 2',12);

  -- =========================
  -- L2 — THAY ĐỔI (45 ngày)
  -- =========================
  perform program.add_step(l2,1,'07:30','Check-in + tư vấn NRT phối hợp',
      'Đánh dấu dùng miếng dán/gum (tham khảo bác sĩ)',12);
  perform program.add_step(l2,1,'12:30','Học 4D + log thử','Tạo Urge log, Timer 3’',8);
  perform program.add_step(l2,1,'16:30','CBT 1 tình huống thật','Điền A–B–C + câu thay thế',12);
  perform program.add_step(l2,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,2,'07:30','Chọn liều NRT tham khảo','Luôn hỏi bác sĩ khi cần',12);
  perform program.add_step(l2,2,'12:30','Urge + Timer 3’',null,10);
  perform program.add_step(l2,2,'16:30','Body-scan 5’',null,12);
  perform program.add_step(l2,2,'21:00','Nhật ký + an toàn NRT','Tick triệu chứng nhẹ nếu có',12);

  perform program.add_step(l2,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l2,3,'12:30','Tình huống xã hội',null,12);
  perform program.add_step(l2,3,'16:30','Thay khung nhận thức',null,12);
  perform program.add_step(l2,3,'21:00','Nhật ký + safety NRT',null,12);

  perform program.add_step(l2,4,'07:30','Áp lịch nhắc cá nhân hoá',null,10);
  perform program.add_step(l2,4,'12:30','Vật thay thế 24h',null,8);
  perform program.add_step(l2,4,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l2,4,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,5,'07:30','Tuyên ngôn hành động',null,10);
  perform program.add_step(l2,5,'12:30','Urge + Quiz 2 câu',null,10);
  perform program.add_step(l2,5,'16:30','Từ chối khéo',null,12);
  perform program.add_step(l2,5,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,6,'07:30','Flow phục hồi','Không reset toàn bộ streak',12);
  perform program.add_step(l2,6,'12:30','Kế hoạch cuối tuần',null,10);
  perform program.add_step(l2,6,'16:30','Mindfulness 5’',null,12);
  perform program.add_step(l2,6,'21:00','Nhật ký + safety',null,12);

  perform program.add_step(l2,7,'07:30','7-day PPA',null,12);
  perform program.add_step(l2,7,'12:30','Top-3 chiến lược',null,10);
  perform program.add_step(l2,7,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l2,7,'21:00','Kế hoạch tuần 2',null,12);

  -- Ngày 8..21
for i in 8..21 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Urge log','Timer 3’ nếu cần',8);
      perform program.add_step(l2,i,'16:30','Kỹ năng phù hợp','Ngủ/giảm stress/exercise/CBT',12);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 22..28
for i in 22..28 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen','Pomodoro/mindfulness/exercise/xã hội',12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps
set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=28 and slot='21:00';

-- Ngày 29..35
for i in 29..35 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=35 and slot='21:00';

-- Ngày 36..42
for i in 36..42 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=42 and slot='21:00';

-- Ngày 43..45
for i in 43..45 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=45 and slot='21:00';

-- =========================
-- L3 — TỰ DO (60 ngày)
-- =========================

-- Ngày 1..3
perform program.add_step(l3,1,'07:30','Check-in nâng cao + tư vấn thuốc','Đánh dấu thuốc/NRT; tuỳ chọn đo mạch',15);
  perform program.add_step(l3,1,'12:30','Checklist môi trường + nhắn người thân',null,10);
  perform program.add_step(l3,1,'16:30','CBT-ABC (case nặng)',null,12);
  perform program.add_step(l3,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l3,2,'07:30','Kế hoạch thuốc','Giờ dán miếng dán; gum dự phòng',12);
  perform program.add_step(l3,2,'12:30','Urge baseline + đặt nhắc',null,10);
  perform program.add_step(l3,2,'16:30','Mindfulness 5’ + đi bộ 5’',null,12);
  perform program.add_step(l3,2,'21:00','Nhật ký + safety baseline',null,12);

  perform program.add_step(l3,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l3,3,'12:30','Urge + Timer 3’ + gọi bạn hỗ trợ','Nếu thèm ≥8, bấm gọi buddy',10);
  perform program.add_step(l3,3,'16:30','CBT thay niềm tin lõi','Viết 1 câu thay thế mạnh',12);
  perform program.add_step(l3,3,'21:00','Nhật ký + kiểm tra tác dụng phụ',null,12);

  -- Ngày 4..14
for i in 4..14 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng (nhắc dày tuần đầu)',null,12);
      perform program.add_step(l3,i,'12:30','Urge Log + 4D','Timer 3’',10);
      perform program.add_step(l3,i,'16:30','Luân phiên bài phù hợp','Thở/mindfulness/pomodoro/xã hội',12);
      perform program.add_step(l3,i,'21:00','Nhật ký + safety (nếu dùng thuốc)',null,12);
end loop;

  -- Ngày 15..28
for i in 15..28 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Tình huống rủi ro cao','Chuẩn bị câu nói/động tác',12);
      perform program.add_step(l3,i,'16:30','Vận động nhẹ 10’','Đi bộ/giãn cơ',12);
      perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 29..42
for i in 29..42 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Kỹ năng xã hội nâng cao','Nói “không” khi đi nhậu/cà phê',12);
      perform program.add_step(l3,i,'16:30','Pomodoro 25–5',null,12);
      perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 43..60
for i in 43..60 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Kế hoạch phòng ngừa tái sử dụng','Chọn 1 bẫy + 1 hành động sẵn sàng',12);
      perform program.add_step(l3,i,'16:30','Củng cố bản sắc + mục tiêu thể lực','Đặt mục tiêu bước chân/đi bộ',12);
      perform program.add_step(l3,i,'21:00','Tổng kết tuần/huân chương','Nếu trùng mốc tuần thì trao huy hiệu',12);
end loop;

end
$seed$;

drop function if exists program.add_step(uuid,int,text,text,text,int);
