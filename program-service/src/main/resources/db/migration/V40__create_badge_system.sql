-- 1. Thêm cột has_paused vào programs
ALTER TABLE program.programs ADD COLUMN IF NOT EXISTS has_paused BOOLEAN DEFAULT FALSE;

-- 2. Tạo bảng badges
CREATE TABLE program.badges (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- PROG_LV1, STREAK_LV2...
    category VARCHAR(20) NOT NULL, -- PROGRAM, STREAK, QUIZ
    level INT NOT NULL, -- 1, 2, 3
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Tạo bảng user_badges
CREATE TABLE program.user_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    badge_id UUID NOT NULL REFERENCES program.badges(id),
    program_id UUID REFERENCES program.programs(id),
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_user_badge_program UNIQUE (user_id, badge_id, program_id) -- Một user chỉ nhận 1 loại huy hiệu 1 lần cho 1 program
);

-- 4. Seed Data
-- Program Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('11111111-1111-1111-1111-111111111111', 'PROG_LV1', 'PROGRAM', 1, 'Khởi Hành', 'Bắt đầu hành trình cai thuốc lá.', 'assets/badges/prog_lv1.png'),
('11111111-1111-1111-1111-111111111112', 'PROG_LV2', 'PROGRAM', 2, 'Kiên Trì', 'Đi được một nửa chặng đường mà không tạm dừng.', 'assets/badges/prog_lv2.png'),
('11111111-1111-1111-1111-111111111113', 'PROG_LV3', 'PROGRAM', 3, 'Về Đích', 'Hoàn thành toàn bộ lộ trình cai thuốc.', 'assets/badges/prog_lv3.png');

-- Streak Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('22222222-2222-2222-2222-222222222221', 'STREAK_LV1', 'STREAK', 1, 'Tuần Lễ Vàng', 'Đạt chuỗi 7 ngày không hút thuốc.', 'assets/badges/streak_lv1.png'),
('22222222-2222-2222-2222-222222222222', 'STREAK_LV2', 'STREAK', 2, 'Thói Quen Mới', 'Đạt chuỗi ngày bằng một nửa lộ trình.', 'assets/badges/streak_lv2.png'),
('22222222-2222-2222-2222-222222222223', 'STREAK_LV3', 'STREAK', 3, 'Chiến Binh Tự Do', 'Giữ vững chuỗi không hút thuốc suốt cả lộ trình.', 'assets/badges/streak_lv3.png');

-- Quiz Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('33333333-3333-3333-3333-333333333331', 'QUIZ_LV1', 'QUIZ', 1, 'Tự Nhận Thức', 'Hoàn thành bài kiểm tra định kỳ đầu tiên.', 'assets/badges/quiz_lv1.png'),
('33333333-3333-3333-3333-333333333332', 'QUIZ_LV2', 'QUIZ', 2, 'Tiến Triển Tốt', 'Có kết quả kiểm tra cải thiện hoặc ổn định 2 lần liên tiếp.', 'assets/badges/quiz_lv2.png'),
('33333333-3333-3333-3333-333333333333', 'QUIZ_LV3', 'QUIZ', 3, 'Làm Chủ', 'Hoàn thành tất cả bài kiểm tra với mức độ phụ thuộc thấp.', 'assets/badges/quiz_lv3.png');
