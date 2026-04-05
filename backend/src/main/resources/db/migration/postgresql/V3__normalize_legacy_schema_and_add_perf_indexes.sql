-- PostgreSQL migration V3
-- Goals:
-- 1) normalize legacy columns to current JPA naming
-- 2) clean redundant historical columns
-- 3) add targeted indexes for slow-query hotspots

DO
$$
BEGIN
  IF to_regclass('public.organizations') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'organizations' AND column_name = 'org_name') THEN
      IF EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'organizations' AND column_name = 'name') THEN
        UPDATE public.organizations
        SET name = COALESCE(name, org_name)
        WHERE org_name IS NOT NULL;
        ALTER TABLE public.organizations DROP COLUMN IF EXISTS org_name;
      ELSE
        ALTER TABLE public.organizations RENAME COLUMN org_name TO name;
      END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'organizations' AND column_name = 'org_type') THEN
      IF EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'organizations' AND column_name = 'type') THEN
        UPDATE public.organizations
        SET type = COALESCE(type, org_type::text)
        WHERE org_type IS NOT NULL;
        ALTER TABLE public.organizations DROP COLUMN IF EXISTS org_type;
      ELSE
        ALTER TABLE public.organizations RENAME COLUMN org_type TO type;
      END IF;
    END IF;
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.user_organizations') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'org_id') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'organization_id') THEN
        ALTER TABLE public.user_organizations ADD COLUMN organization_id VARCHAR(255);
      END IF;

      UPDATE public.user_organizations
      SET organization_id = COALESCE(organization_id, org_id::text)
      WHERE org_id IS NOT NULL;

      ALTER TABLE public.user_organizations DROP COLUMN IF EXISTS org_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'member_role') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'role_in_org') THEN
        ALTER TABLE public.user_organizations ADD COLUMN role_in_org VARCHAR(20);
      END IF;

      UPDATE public.user_organizations
      SET role_in_org = COALESCE(role_in_org, member_role::text)
      WHERE member_role IS NOT NULL;

      ALTER TABLE public.user_organizations DROP COLUMN IF EXISTS member_role;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'joined_date') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'user_organizations' AND column_name = 'joined_at') THEN
        ALTER TABLE public.user_organizations ADD COLUMN joined_at TIMESTAMP WITH TIME ZONE;
      END IF;

      UPDATE public.user_organizations
      SET joined_at = COALESCE(joined_at, joined_date::timestamptz)
      WHERE joined_date IS NOT NULL;

      ALTER TABLE public.user_organizations DROP COLUMN IF EXISTS joined_date;
    END IF;
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.exam_enrollments') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'exam_enrollments' AND column_name = 'enrollment_status') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'exam_enrollments' AND column_name = 'status') THEN
        ALTER TABLE public.exam_enrollments ADD COLUMN status VARCHAR(255);
      END IF;

      UPDATE public.exam_enrollments
      SET status = COALESCE(status, enrollment_status::text)
      WHERE enrollment_status IS NOT NULL;

      ALTER TABLE public.exam_enrollments DROP COLUMN IF EXISTS enrollment_status;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'exam_enrollments' AND column_name = 'attempt_count') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'exam_enrollments' AND column_name = 'attempts_used') THEN
        ALTER TABLE public.exam_enrollments ADD COLUMN attempts_used INTEGER;
      END IF;

      UPDATE public.exam_enrollments
      SET attempts_used = COALESCE(attempts_used, attempt_count)
      WHERE attempt_count IS NOT NULL;

      ALTER TABLE public.exam_enrollments DROP COLUMN IF EXISTS attempt_count;
    END IF;
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.question_versions') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'version_num') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'version') THEN
        ALTER TABLE public.question_versions ADD COLUMN version INTEGER;
      END IF;

      UPDATE public.question_versions
      SET version = COALESCE(version, version_num)
      WHERE version_num IS NOT NULL;

      ALTER TABLE public.question_versions DROP COLUMN IF EXISTS version_num;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'options') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'options_json') THEN
        ALTER TABLE public.question_versions ADD COLUMN options_json TEXT;
      END IF;

      UPDATE public.question_versions
      SET options_json = COALESCE(options_json, options::text)
      WHERE options IS NOT NULL;

      ALTER TABLE public.question_versions DROP COLUMN IF EXISTS options;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'answer') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'answer_schema') THEN
        ALTER TABLE public.question_versions ADD COLUMN answer_schema TEXT;
      END IF;

      UPDATE public.question_versions
      SET answer_schema = COALESCE(answer_schema, answer)
      WHERE answer IS NOT NULL;

      ALTER TABLE public.question_versions DROP COLUMN IF EXISTS answer;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'created_by') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'changed_by') THEN
        ALTER TABLE public.question_versions ADD COLUMN changed_by VARCHAR(255);
      END IF;

      UPDATE public.question_versions
      SET changed_by = COALESCE(changed_by, created_by::text)
      WHERE created_by IS NOT NULL;

      ALTER TABLE public.question_versions DROP COLUMN IF EXISTS created_by;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'created_at') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'question_versions' AND column_name = 'changed_at') THEN
        ALTER TABLE public.question_versions ADD COLUMN changed_at TIMESTAMP WITH TIME ZONE;
      END IF;

      UPDATE public.question_versions
      SET changed_at = COALESCE(changed_at, created_at::timestamptz)
      WHERE created_at IS NOT NULL;

      ALTER TABLE public.question_versions DROP COLUMN IF EXISTS created_at;
    END IF;
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.role_permissions') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'role_permissions' AND column_name = 'perm_id') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'role_permissions' AND column_name = 'permission_id') THEN
        ALTER TABLE public.role_permissions ADD COLUMN permission_id VARCHAR(255);
      END IF;

      UPDATE public.role_permissions
      SET permission_id = COALESCE(permission_id, perm_id::text)
      WHERE perm_id IS NOT NULL;

      ALTER TABLE public.role_permissions DROP COLUMN IF EXISTS perm_id;
    END IF;

    CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON public.role_permissions (role_id);
    CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON public.role_permissions (permission_id);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.user_roles') IS NOT NULL THEN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_roles' AND column_name = 'org_id') THEN
      IF NOT EXISTS (
          SELECT 1
          FROM information_schema.columns
          WHERE table_schema = 'public' AND table_name = 'user_roles' AND column_name = 'scope_org_id') THEN
        ALTER TABLE public.user_roles ADD COLUMN scope_org_id VARCHAR(255);
      END IF;

      UPDATE public.user_roles
      SET scope_org_id = COALESCE(scope_org_id, org_id::text)
      WHERE org_id IS NOT NULL;

      ALTER TABLE public.user_roles DROP COLUMN IF EXISTS org_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_roles' AND column_name = 'scope_org_id') THEN
      UPDATE public.user_roles
      SET scope_org_id = COALESCE(scope_org_id, '');
      ALTER TABLE public.user_roles ALTER COLUMN scope_org_id SET DEFAULT '';
      ALTER TABLE public.user_roles ALTER COLUMN scope_org_id SET NOT NULL;
    END IF;

    CREATE INDEX IF NOT EXISTS idx_user_roles_user_scope ON public.user_roles (user_id, scope_org_id);
    CREATE INDEX IF NOT EXISTS idx_user_roles_role ON public.user_roles (role_id);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.announcements') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_announcement_status_created
      ON public.announcements (status, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_announcement_status_priority_published
      ON public.announcements (status, priority, published_at DESC);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.user_notifications') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_notification_receiver_type_created
      ON public.user_notifications (receiver_id, type, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_notification_chat_lookup
      ON public.user_notifications (receiver_id, sender_id, type, is_read, created_at DESC);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.exam_records') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_exam_records_exam ON public.exam_records (exam_id);
    CREATE INDEX IF NOT EXISTS idx_exam_records_exam_question ON public.exam_records (exam_id, question_id);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.paper_items') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_paper_items_paper_sort ON public.paper_items (paper_id, sort_order);
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.knowledge_points') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_knowledge_points_subject_sort ON public.knowledge_points (subject_id, sort_order);
    CREATE INDEX IF NOT EXISTS idx_knowledge_points_parent_sort ON public.knowledge_points (parent_id, sort_order);
  END IF;
END
$$;
