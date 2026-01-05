-- P0 Feature Tables
-- This script creates tables for core functionality: RBAC, Organization, Question Review, Exam Plan, Export

-- 组织管理表
CREATE TABLE IF NOT EXISTS organizations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT COMMENT '父级组织ID',
  org_name VARCHAR(255) NOT NULL COMMENT '组织名称',
  org_type ENUM('SCHOOL','DEPARTMENT','CLASS') NOT NULL COMMENT '组织类型',
  code VARCHAR(100) UNIQUE COMMENT '组织编码',
  description TEXT COMMENT '描述',
  sort_order INT DEFAULT 0 COMMENT '排序',
  status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE' COMMENT '状态',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT COMMENT '创建人',
  FOREIGN KEY (parent_id) REFERENCES organizations(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_parent_id (parent_id),
  INDEX idx_org_type (org_type),
  INDEX idx_status (status)
);

-- 课程表
CREATE TABLE IF NOT EXISTS courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department_id BIGINT COMMENT '所属学院ID',
  course_name VARCHAR(255) NOT NULL COMMENT '课程名称',
  course_code VARCHAR(100) UNIQUE COMMENT '课程代码',
  credit INT COMMENT '学分',
  semester VARCHAR(20) COMMENT '学期',
  description TEXT COMMENT '描述',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT COMMENT '创建人',
  FOREIGN KEY (department_id) REFERENCES organizations(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_department_id (department_id),
  INDEX idx_semester (semester)
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(100) NOT NULL UNIQUE COMMENT '角色名称',
  role_code VARCHAR(100) NOT NULL UNIQUE COMMENT '角色编码',
  description TEXT COMMENT '描述',
  is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统内置角色',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_role_code (role_code)
);

-- 权限点表
CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_name VARCHAR(255) NOT NULL COMMENT '权限名称',
  perm_code VARCHAR(200) NOT NULL UNIQUE COMMENT '权限编码（resource:action）',
  resource VARCHAR(100) COMMENT '资源',
  action VARCHAR(100) COMMENT '操作',
  scope VARCHAR(100) DEFAULT 'ORG' COMMENT '作用域：ORG/GLOBAL',
  description TEXT COMMENT '描述',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_perm_code (perm_code),
  INDEX idx_resource (resource)
);

-- 角色权限关系表
CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  scope VARCHAR(100) DEFAULT 'ORG' COMMENT '权限作用范围',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY (perm_id) REFERENCES permissions(id) ON DELETE CASCADE,
  UNIQUE KEY unique_role_perm (role_id, perm_id),
  INDEX idx_role_id (role_id),
  INDEX idx_perm_id (perm_id)
);

-- 用户角色关系表
CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  org_id BIGINT COMMENT '角色作用的组织范围',
  effective_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  expiration_date DATETIME COMMENT '过期时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY (org_id) REFERENCES organizations(id),
  UNIQUE KEY unique_user_role_org (user_id, role_id, org_id),
  INDEX idx_user_id (user_id),
  INDEX idx_role_id (role_id)
);

-- 用户组织关系表
CREATE TABLE IF NOT EXISTS user_organizations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  org_id BIGINT NOT NULL,
  member_role ENUM('MEMBER','MANAGER','OWNER') DEFAULT 'MEMBER' COMMENT '组织内角色',
  joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE CASCADE,
  UNIQUE KEY unique_user_org (user_id, org_id),
  INDEX idx_user_id (user_id),
  INDEX idx_org_id (org_id)
);

-- 教师授课班级关系表
CREATE TABLE IF NOT EXISTS teacher_classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL COMMENT '班级ID',
  semester VARCHAR(20),
  assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  FOREIGN KEY (teacher_id) REFERENCES users(id),
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (class_id) REFERENCES organizations(id),
  INDEX idx_teacher_id (teacher_id),
  INDEX idx_course_id (course_id),
  INDEX idx_class_id (class_id)
);

-- 题目版本表
CREATE TABLE IF NOT EXISTS question_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  version_num INT NOT NULL COMMENT '版本号',
  stem TEXT NOT NULL COMMENT '题干',
  options JSON COMMENT '选项（多选题存JSON）',
  answer VARCHAR(255) COMMENT '标准答案',
  analysis TEXT COMMENT '解析',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT,
  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
  UNIQUE KEY unique_question_version (question_id, version_num),
  INDEX idx_question_id (question_id)
);

-- 题目审核表
CREATE TABLE IF NOT EXISTS question_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  version BIGINT NOT NULL COMMENT '审核的版本号',
  status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '审核状态',
  reviewer_id BIGINT COMMENT '审核人',
  review_notes TEXT COMMENT '审核意见',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME COMMENT '审核时间',
  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
  FOREIGN KEY (reviewer_id) REFERENCES users(id),
  INDEX idx_question_id (question_id),
  INDEX idx_status (status),
  INDEX idx_reviewer_id (reviewer_id)
);

-- 考试计划表
CREATE TABLE IF NOT EXISTS exam_plans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_name VARCHAR(255) NOT NULL COMMENT '考试名称',
  exam_code VARCHAR(100) UNIQUE COMMENT '考试代码',
  course_id BIGINT COMMENT '关联课程',
  exam_type ENUM('FORMAL','MAKEUP','RETAKE') DEFAULT 'FORMAL' COMMENT '考试类型',
  status ENUM('DRAFT','PUBLISHED','ONGOING','FINISHED','CANCELLED') DEFAULT 'DRAFT' COMMENT '状态',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  duration_mins INT DEFAULT 120 COMMENT '考试时长（分钟）',
  pass_score INT DEFAULT 60 COMMENT '及格分数',
  total_score INT DEFAULT 100 COMMENT '总分',
  max_attempts INT DEFAULT 1 COMMENT '最大尝试次数',
  description TEXT COMMENT '描述',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT COMMENT '创建人',
  published_by BIGINT COMMENT '发布人',
  published_at DATETIME COMMENT '发布时间',
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  FOREIGN KEY (published_by) REFERENCES users(id),
  INDEX idx_exam_type (exam_type),
  INDEX idx_status (status),
  INDEX idx_course_id (course_id),
  INDEX idx_start_time (start_time)
);

-- 考试班级关系表
CREATE TABLE IF NOT EXISTS exam_plan_classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_plan_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  enrollment_status ENUM('ENROLLED','EXEMPTED','ABSENT') DEFAULT 'ENROLLED' COMMENT '报名状态',
  assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (exam_plan_id) REFERENCES exam_plans(id) ON DELETE CASCADE,
  FOREIGN KEY (class_id) REFERENCES organizations(id),
  UNIQUE KEY unique_exam_class (exam_plan_id, class_id),
  INDEX idx_exam_plan_id (exam_plan_id),
  INDEX idx_class_id (class_id)
);

-- 考试报名表
CREATE TABLE IF NOT EXISTS exam_enrollments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_plan_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  enrollment_status ENUM('ENROLLED','EXEMPTED','ABSENT') DEFAULT 'ENROLLED' COMMENT '报名状态',
  seat_number VARCHAR(100) COMMENT '座位号',
  attempt_count INT DEFAULT 0 COMMENT '参加次数',
  score DECIMAL(5,2) COMMENT '成绩',
  enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME COMMENT '考试完成时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (exam_plan_id) REFERENCES exam_plans(id) ON DELETE CASCADE,
  FOREIGN KEY (student_id) REFERENCES users(id),
  UNIQUE KEY unique_exam_student (exam_plan_id, student_id),
  INDEX idx_exam_plan_id (exam_plan_id),
  INDEX idx_student_id (student_id),
  INDEX idx_enrollment_status (enrollment_status)
);

-- 导出任务表
CREATE TABLE IF NOT EXISTS export_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_name VARCHAR(255) NOT NULL COMMENT '导出任务名称',
  task_type ENUM('PAPER_PDF','PAPER_WORD','ANSWER_SHEET','GRADE_REPORT') COMMENT '导出类型',
  status ENUM('PENDING','PROCESSING','DONE','FAILED') DEFAULT 'PENDING' COMMENT '任务状态',
  file_path VARCHAR(255) COMMENT '生成文件路径',
  error_message TEXT COMMENT '错误信息',
  progress INT DEFAULT 0 COMMENT '进度百分比',
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  started_at DATETIME COMMENT '开始处理时间',
  finished_at DATETIME COMMENT '完成时间',
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_task_type (task_type),
  INDEX idx_status (status),
  INDEX idx_created_by (created_by),
  INDEX idx_created_at (created_at)
);

-- 扩展用户表字段（如果还未扩展）
-- ALTER TABLE users ADD COLUMN dept_id BIGINT COMMENT '所属部门';
-- ALTER TABLE users ADD COLUMN job_title VARCHAR(100) COMMENT '职位';
-- ALTER TABLE users ADD CONSTRAINT fk_users_dept FOREIGN KEY (dept_id) REFERENCES organizations(id);

-- 初始化系统内置角色
INSERT IGNORE INTO roles (role_name, role_code, description, is_system) VALUES
('系统管理员', 'ADMIN', '系统管理员，拥有所有权限', TRUE),
('教师', 'TEACHER', '教师角色，可以管理题目和考试', TRUE),
('学生', 'STUDENT', '学生角色，可以参加考试和练习', TRUE),
('出卷教师', 'PAPER_CREATOR', '专业出卷教师', TRUE),
('审核员', 'REVIEWER', '题目审核员', TRUE);

-- 初始化权限点
INSERT IGNORE INTO permissions (perm_name, perm_code, resource, action, scope, description) VALUES
-- 组织管理权限
('组织创建', 'org:create', 'org', 'create', 'ORG', '创建组织'),
('组织编辑', 'org:update', 'org', 'update', 'ORG', '编辑组织'),
('组织删除', 'org:delete', 'org', 'delete', 'ORG', '删除组织'),
('组织查看', 'org:view', 'org', 'view', 'ORG', '查看组织'),
-- 角色权限管理
('角色管理', 'role:manage', 'role', 'manage', 'GLOBAL', '管理系统角色'),
('权限分配', 'role:assign', 'role', 'assign', 'GLOBAL', '为用户分配角色'),
-- 题目管理权限
('题目创建', 'question:create', 'question', 'create', 'ORG', '创建题目'),
('题目编辑', 'question:update', 'question', 'update', 'ORG', '编辑题目'),
('题目删除', 'question:delete', 'question', 'delete', 'ORG', '删除题目'),
('题目查看', 'question:view', 'question', 'view', 'ORG', '查看题目'),
('题目导入', 'question:import', 'question', 'import', 'ORG', '导入题目'),
-- 题目审核权限
('题目审核', 'question:review', 'question', 'review', 'ORG', '审核题目'),
('审核通过', 'question:approve', 'question', 'approve', 'ORG', '审核通过题目'),
('审核退回', 'question:reject', 'question', 'reject', 'ORG', '审核退回题目'),
-- 试卷管理权限
('试卷创建', 'paper:create', 'paper', 'create', 'ORG', '创建试卷'),
('试卷编辑', 'paper:update', 'paper', 'update', 'ORG', '编辑试卷'),
('试卷删除', 'paper:delete', 'paper', 'delete', 'ORG', '删除试卷'),
('试卷查看', 'paper:view', 'paper', 'view', 'ORG', '查看试卷'),
('自动组卷', 'paper:auto_generate', 'paper', 'auto_generate', 'ORG', '自动组卷'),
-- 考试管理权限
('考试创建', 'exam:create', 'exam', 'create', 'ORG', '创建考试计划'),
('考试编辑', 'exam:update', 'exam', 'update', 'ORG', '编辑考试计划'),
('考试删除', 'exam:delete', 'exam', 'delete', 'ORG', '删除考试计划'),
('考试查看', 'exam:view', 'exam', 'view', 'ORG', '查看考试计划'),
('考试发布', 'exam:publish', 'exam', 'publish', 'ORG', '发布考试计划'),
('报名管理', 'exam:enroll', 'exam', 'enroll', 'ORG', '管理学生报名'),
('缺考标记', 'exam:mark_absent', 'exam', 'mark_absent', 'ORG', '标记学生缺考'),
-- 成绩管理权限
('成绩查看', 'grade:view', 'grade', 'view', 'ORG', '查看成绩'),
('成绩导出', 'grade:export', 'grade', 'export', 'ORG', '导出成绩'),
-- 导出权限
('文档导出', 'export:document', 'export', 'document', 'ORG', '导出为文档'),
('数据导出', 'export:data', 'export', 'data', 'ORG', '导出为数据'),
-- 系统管理权限
('系统设置', 'system:config', 'system', 'config', 'GLOBAL', '系统配置'),
('日志查看', 'system:logs', 'system', 'logs', 'GLOBAL', '查看系统日志'),
('用户管理', 'system:user', 'system', 'user', 'GLOBAL', '用户管理');

-- 为ADMIN角色分配所有权限
INSERT IGNORE INTO role_permissions (role_id, perm_id, scope)
SELECT r.id, p.id, 'GLOBAL'
FROM roles r
JOIN permissions p ON TRUE
WHERE r.role_code = 'ADMIN';

-- 为TEACHER角色分配题目、试卷、考试权限
INSERT IGNORE INTO role_permissions (role_id, perm_id, scope)
SELECT r.id, p.id, 'ORG'
FROM roles r
JOIN permissions p ON p.perm_code IN (
  'question:create', 'question:update', 'question:delete', 'question:view', 'question:import',
  'paper:create', 'paper:update', 'paper:delete', 'paper:view', 'paper:auto_generate',
  'exam:create', 'exam:update', 'exam:delete', 'exam:view', 'exam:publish', 'exam:enroll', 'exam:mark_absent',
  'grade:view', 'grade:export', 'export:document', 'export:data'
)
WHERE r.role_code = 'TEACHER';

-- 为STUDENT角色分配查看权限
INSERT IGNORE INTO role_permissions (role_id, perm_id, scope)
SELECT r.id, p.id, 'ORG'
FROM roles r
JOIN permissions p ON p.perm_code IN ('exam:view', 'grade:view', 'paper:view')
WHERE r.role_code = 'STUDENT';

-- 为REVIEWER角色分配审核权限
INSERT IGNORE INTO role_permissions (role_id, perm_id, scope)
SELECT r.id, p.id, 'ORG'
FROM roles r
JOIN permissions p ON p.perm_code IN (
  'question:review', 'question:approve', 'question:reject', 'question:view'
)
WHERE r.role_code = 'REVIEWER';

-- 为PAPER_CREATOR角色分配出卷权限
INSERT IGNORE INTO role_permissions (role_id, perm_id, scope)
SELECT r.id, p.id, 'ORG'
FROM roles r
JOIN permissions p ON p.perm_code IN (
  'question:create', 'question:update', 'question:view',
  'paper:create', 'paper:update', 'paper:view', 'paper:auto_generate',
  'export:document'
)
WHERE r.role_code = 'PAPER_CREATOR';
