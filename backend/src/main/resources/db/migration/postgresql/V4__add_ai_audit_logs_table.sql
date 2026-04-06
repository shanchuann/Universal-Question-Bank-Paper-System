-- PostgreSQL migration V4
-- Add ai_audit_logs for AI audit trail and acceptance analytics.

DO
$$
BEGIN
  IF to_regclass('public.ai_audit_logs') IS NULL THEN
    CREATE TABLE public.ai_audit_logs (
      id VARCHAR(255) PRIMARY KEY,
      feature VARCHAR(80) NOT NULL,
      user_id VARCHAR(255) NOT NULL,
      user_role VARCHAR(255),
      prompt_text VARCHAR(5000),
      response_text VARCHAR(8000),
      context_text VARCHAR(3000),
      context_ref VARCHAR(120),
      model_name VARCHAR(100),
      success BOOLEAN,
      error_message VARCHAR(1000),
      latency_ms BIGINT,
      accepted BOOLEAN,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL
    );
  END IF;
END
$$;

DO
$$
BEGIN
  IF to_regclass('public.ai_audit_logs') IS NOT NULL THEN
    CREATE INDEX IF NOT EXISTS idx_ai_audit_user_created
      ON public.ai_audit_logs (user_id, created_at);

    CREATE INDEX IF NOT EXISTS idx_ai_audit_feature_created
      ON public.ai_audit_logs (feature, created_at);

    CREATE INDEX IF NOT EXISTS idx_ai_audit_accepted
      ON public.ai_audit_logs (accepted);
  END IF;
END
$$;
