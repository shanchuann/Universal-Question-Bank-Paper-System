-- PostgreSQL schema optimization migration.
-- Goal: add high-value indexes and uniqueness constraints for hot query paths.

DO
$$
BEGIN
  IF to_regclass('public.questions') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_question_status_created ON public.questions (status, created_at);
    CREATE INDEX IF NOT EXISTS idx_question_creator_status_created ON public.questions (created_by, status, created_at);
    CREATE INDEX IF NOT EXISTS idx_question_org_status_created ON public.questions (organization_id, status, created_at);
    CREATE INDEX IF NOT EXISTS idx_question_subject_type_diff ON public.questions (subject_id, type, difficulty);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.question_knowledge_points') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_qkp_question ON public.question_knowledge_points (question_id);
    CREATE INDEX IF NOT EXISTS idx_qkp_knowledge_point ON public.question_knowledge_points (knowledge_point_id);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.operation_logs') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_operation_log_timestamp ON public.operation_logs ("timestamp");
    CREATE INDEX IF NOT EXISTS idx_operation_log_action_timestamp ON public.operation_logs (action, "timestamp");
    CREATE INDEX IF NOT EXISTS idx_operation_log_user_timestamp ON public.operation_logs (user_id, "timestamp");
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.exams') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_exam_user_end ON public.exams (user_id, end_time);
    CREATE INDEX IF NOT EXISTS idx_exam_user_start ON public.exams (user_id, start_time);
    CREATE INDEX IF NOT EXISTS idx_exam_paper_start ON public.exams (paper_id, start_time);
    CREATE INDEX IF NOT EXISTS idx_exam_grading_start ON public.exams (grading_status, start_time);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.exam_plans') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_exam_plan_status_start ON public.exam_plans (status, start_time);
    CREATE INDEX IF NOT EXISTS idx_exam_plan_status_end ON public.exam_plans (status, end_time);
    CREATE INDEX IF NOT EXISTS idx_exam_plan_creator_created ON public.exam_plans (created_by, created_at);
    CREATE INDEX IF NOT EXISTS idx_exam_plan_course_status ON public.exam_plans (course_id, status);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.users') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_user_role ON public.users (role);
    CREATE INDEX IF NOT EXISTS idx_user_email ON public.users (email);
    CREATE INDEX IF NOT EXISTS idx_user_created_at ON public.users (created_at);
  END IF;
END
$$;

DO
$$
DECLARE
  org_column text;
  has_duplicates boolean;
BEGIN
  IF to_regclass('public.user_organizations') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_user_org_user ON public.user_organizations (user_id);

    SELECT CASE
             WHEN EXISTS (
               SELECT 1
               FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name = 'user_organizations'
                 AND column_name = 'organization_id') THEN 'organization_id'
             WHEN EXISTS (
               SELECT 1
               FROM information_schema.columns
               WHERE table_schema = 'public'
                 AND table_name = 'user_organizations'
                 AND column_name = 'org_id') THEN 'org_id'
             ELSE NULL
           END
      INTO org_column;

    IF org_column IS NOT NULL THEN
      EXECUTE format(
          'CREATE INDEX IF NOT EXISTS idx_user_org_org ON public.user_organizations (%I)',
          org_column);

      EXECUTE format(
          'CREATE INDEX IF NOT EXISTS idx_user_org_org_user ON public.user_organizations (%I, user_id)',
          org_column);

      EXECUTE format(
          'SELECT EXISTS (SELECT 1 FROM public.user_organizations GROUP BY user_id, %I HAVING COUNT(*) > 1)',
          org_column)
      INTO has_duplicates;

      IF NOT has_duplicates THEN
        EXECUTE format(
            'CREATE UNIQUE INDEX IF NOT EXISTS uk_user_organization_pair ON public.user_organizations (user_id, %I)',
            org_column);
      ELSE
        RAISE NOTICE 'skip creating uk_user_organization_pair because duplicate rows exist';
      END IF;
    END IF;
  END IF;
END
$$;


DO
$$
BEGIN
  IF to_regclass('public.exam_enrollments') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_exam_enrollment_plan ON public.exam_enrollments (exam_plan_id);
    CREATE INDEX IF NOT EXISTS idx_exam_enrollment_student ON public.exam_enrollments (student_id);
    CREATE INDEX IF NOT EXISTS idx_exam_enrollment_plan_status ON public.exam_enrollments (exam_plan_id, status);

    IF NOT EXISTS (
      SELECT 1
      FROM public.exam_enrollments
      GROUP BY exam_plan_id, student_id
      HAVING COUNT(*) > 1
    ) THEN
      CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_enrollment_plan_student
        ON public.exam_enrollments (exam_plan_id, student_id);
    ELSE
      RAISE NOTICE 'skip creating uk_exam_enrollment_plan_student because duplicate rows exist';
    END IF;
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.exam_plan_classes') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_exam_plan_class_class_id ON public.exam_plan_classes (class_id);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.organizations') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_org_parent_sort ON public.organizations (parent_id, sort_order);
    CREATE INDEX IF NOT EXISTS idx_org_type_status ON public.organizations (type, status);
    CREATE INDEX IF NOT EXISTS idx_org_created_by ON public.organizations (created_by);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.question_reviews') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_question_review_question_created ON public.question_reviews (question_id, created_at);
    CREATE INDEX IF NOT EXISTS idx_question_review_reviewer_created ON public.question_reviews (reviewer_id, created_at);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.export_tasks') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_export_task_creator_created ON public.export_tasks (created_by, created_at);
    CREATE INDEX IF NOT EXISTS idx_export_task_status_created ON public.export_tasks (status, created_at);
    CREATE INDEX IF NOT EXISTS idx_export_task_paper ON public.export_tasks (paper_id);
    CREATE INDEX IF NOT EXISTS idx_export_task_exam_plan ON public.export_tasks (exam_plan_id);
  END IF;
END
$$;