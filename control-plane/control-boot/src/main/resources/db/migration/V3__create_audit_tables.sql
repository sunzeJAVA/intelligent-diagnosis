CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS audit_entry (
    id            VARCHAR(64) PRIMARY KEY,
    trace_id      VARCHAR(64),
    user_id       VARCHAR(128) NOT NULL,
    tenant_id     VARCHAR(128) NOT NULL,
    action        VARCHAR(32)  NOT NULL,
    resource      VARCHAR(128),
    resource_id   VARCHAR(128),
    result        VARCHAR(32),
    reason        TEXT,
    context       JSONB,
    ip_address    VARCHAR(64),
    user_agent    TEXT,
    timestamp     TIMESTAMPTZ  NOT NULL,
    completed_at  TIMESTAMPTZ,
    signature     VARCHAR(512),

    CONSTRAINT chk_audit_entry_action CHECK (action IN (
        'CODE_PARSE', 'INDEX_CREATE', 'INDEX_UPDATE', 'INDEX_DELETE', 'INDEX_ROLLBACK',
        'DIAGNOSIS_REQUEST', 'DIAGNOSIS_APPROVE', 'DIAGNOSIS_EXPORT',
        'CONFIG_READ', 'CONFIG_WRITE', 'POLICY_CHANGE'
    )),
    CONSTRAINT chk_audit_entry_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_audit_entry_resource ON audit_entry(resource);
CREATE INDEX IF NOT EXISTS idx_audit_entry_user_id ON audit_entry(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entry_timestamp ON audit_entry(timestamp);
