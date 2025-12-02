-- V33: Chèn dữ liệu cấu hình ban đầu cho tính năng phục hồi streak.
-- File này đảm bảo rằng hệ thống luôn có các module được định nghĩa cho 3 lần phục hồi đầu tiên.

INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
(1, 'RECOVERY_TASK_1'),
(2, 'RECOVERY_TASK_2'),
(3, 'RECOVERY_TASK_3');
