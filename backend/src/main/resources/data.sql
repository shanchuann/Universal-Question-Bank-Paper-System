-- PostgreSQL 初始化数据
-- 此脚本在应用启动时运行，仅插入不存在的数据

-- 插入示例学校
INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440001', '示范大学', 'DEMO_UNIV', 'SCHOOL', NULL, 1, 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440001');

-- 插入示例学院
INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440002', '计算机学院', 'CS_DEPT', 'DEPARTMENT', '550e8400-e29b-41d4-a716-446655440001', 1, 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440002');

INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440003', '数学学院', 'MATH_DEPT', 'DEPARTMENT', '550e8400-e29b-41d4-a716-446655440001', 2, 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440003');

-- 插入示例班级
INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440011', '计算机2024-1班', 'CS2024-1', 'CLASS', '550e8400-e29b-41d4-a716-446655440002', 1, 'ACTIVE', 'CS2024A', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440011');

INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440012', '计算机2024-2班', 'CS2024-2', 'CLASS', '550e8400-e29b-41d4-a716-446655440002', 2, 'ACTIVE', 'CS2024B', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440012');

INSERT INTO organizations (id, name, code, type, parent_id, sort_order, status, invite_code, created_at, updated_at)
SELECT '550e8400-e29b-41d4-a716-446655440013', '数学2024-1班', 'MATH2024-1', 'CLASS', '550e8400-e29b-41d4-a716-446655440003', 1, 'ACTIVE', 'MATH2024A', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '550e8400-e29b-41d4-a716-446655440013');
