-- V1.2 添加邀请码字段和题目归属班级

-- organizations 表添加邀请码字段
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS invite_code VARCHAR(20) UNIQUE;

-- questions 表添加组织ID字段（用于班级题库隔离）
-- NULL 表示共用题库，有值表示归属特定班级
ALTER TABLE questions ADD COLUMN IF NOT EXISTS organization_id VARCHAR(36);
ALTER TABLE questions ADD INDEX IF NOT EXISTS idx_questions_org (organization_id);

-- 为已有班级生成邀请码
UPDATE organizations 
SET invite_code = UPPER(SUBSTRING(MD5(RAND()), 1, 6))
WHERE type = 'CLASS' AND invite_code IS NULL;
