-- Migration V30: Thêm template information vào programs table
-- Mục đích: Lưu thông tin plan template (code, name) để hiển thị dashboard
--
-- Flow:
-- 1. Khi customer start enrollment, program được tạo với template info
-- 2. MeService sử dụng template code/name để hiển thị trong dashboard
-- 3. Không cần join với plan_templates table

BEGIN;

-- Thêm columns cho plan template information
ALTER TABLE program.programs
ADD COLUMN IF NOT EXISTS plan_template_id UUID,
ADD COLUMN IF NOT EXISTS template_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS template_name VARCHAR(255);

-- Tạo index cho plan_template_id
CREATE INDEX IF NOT EXISTS idx_programs_plan_template_id
ON program.programs(plan_template_id);

-- Tạo index cho template_code (có thể cần join theo code)
CREATE INDEX IF NOT EXISTS idx_programs_template_code
ON program.programs(template_code);

-- Foreign key constraint (optional - nếu muốn enforce referential integrity)
-- Nếu uncomment dòng dưới, hãy chắc chắn plan_templates table tồn tại
-- ALTER TABLE program.programs
-- ADD CONSTRAINT fk_programs_plan_template_id
-- FOREIGN KEY (plan_template_id)
-- REFERENCES program.plan_templates(id) ON DELETE SET NULL;

COMMIT;

