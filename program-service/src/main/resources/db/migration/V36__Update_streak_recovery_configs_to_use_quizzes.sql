-- V36: Cập nhật cấu hình phục hồi streak để sử dụng QUIZ cho cả 3 lần thử.
-- File này sẽ ghi đè lên dữ liệu đã được seed bởi V33.

-- Xóa toàn bộ cấu hình cũ để đảm bảo tính nhất quán và có thể chạy lại an toàn.
DELETE FROM program.streak_recovery_configs;

-- Chèn lại cấu hình mới, trong đó cả 3 lần đều là QUIZ.
INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
( 1, 'RECOVERY_QUIZ_1'),
(2, 'RECOVERY_QUIZ_2'),
(3, 'RECOVERY_QUIZ_3');
