-- V34: Cleanup duplicate plan steps caused by V28 and V29
-- This script removes duplicate entries in program.plan_steps for the seeded templates,
-- keeping only one instance for each unique step.

WITH duplicates AS (
    SELECT
        id,
        ROW_NUMBER() OVER(
            PARTITION BY template_id, day_no, slot, title
            ORDER BY id
        ) AS rn
    FROM
        program.plan_steps
    WHERE
        template_id IN (
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222222',
            '33333333-3333-3333-3333-333333333333'
        )
)
DELETE FROM
    program.plan_steps
WHERE
    id IN (SELECT id FROM duplicates WHERE rn > 1);
