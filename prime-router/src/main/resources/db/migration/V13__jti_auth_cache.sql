/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

/* 
 * This SQL creates a table to track JTIs - JWT Identifiers 
 * See "jti" at https://tools.ietf.org/html/rfc7519
 */

-- Two use cases:
-- 1) Has my jti already been previously used
-- 2) Delete jti's whose time has expired.
CREATE TABLE jti_cache (
    jti_cache_id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()+ (5 * interval '1 minute'),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX jti_cache_jti_idx ON jti_cache(jti);


/*
  Add new actions 'resend' and 'rebatch', and fix the current task table to use it.
 */
ALTER TABLE task        ALTER COLUMN next_action TYPE VARCHAR(255);
ALTER TABLE report_file ALTER COLUMN next_action TYPE VARCHAR(255);
ALTER TABLE action      ALTER COLUMN action_name TYPE VARCHAR(255);
DROP TYPE IF EXISTS task_action;
CREATE TYPE task_action AS ENUM ('receive', 'translate', 'batch', 'send', 'download', 'wipe', 'batch_error', 'send_error', 'wipe_error', 'none', 'resend', 'rebatch', 'token_auth', 'token_error');
ALTER TABLE task        ALTER COLUMN next_action TYPE task_action USING next_action::task_action;
ALTER TABLE report_file ALTER COLUMN next_action TYPE task_action USING next_action::task_action;
ALTER TABLE action      ALTER COLUMN action_name TYPE task_action USING action_name::task_action;
