-- V32: Add Streak Recovery Feature

-- 1. Thêm cột đếm số lần phục hồi vào bảng programs
ALTER TABLE program.programs
ADD COLUMN streak_recovery_used_count INT NOT NULL DEFAULT 0;

-- 2. Thêm các cột mới vào bảng step_assignments để hỗ trợ nhiệm vụ phục hồi
ALTER TABLE program.step_assignments
ADD COLUMN assignment_type VARCHAR(255) NOT NULL DEFAULT 'REGULAR',
ADD COLUMN streak_break_id UUID,
ADD COLUMN module_code VARCHAR(255),
ADD COLUMN module_version VARCHAR(255),
ADD COLUMN title_override VARCHAR(255);
-- 3. Tạo bảng cấu hình cho các module phục hồi
CREATE TABLE program.streak_recovery_configs (
    attempt_order INT PRIMARY KEY,
    module_code VARCHAR(255) NOT NULL
);

-- 4. (Tùy chọn) Thêm dữ liệu mẫu cho bảng cấu hình
-- Bạn cần đảm bảo các module với mã này đã tồn tại trong bảng content_modules
-- INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
-- (1, 'RECOVERY_TASK_1'),
-- (2, 'RECOVERY_TASK_2'),
-- (3, 'RECOVERY_TASK_3');