CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS repository_config (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    display_name    VARCHAR(256),
    type            VARCHAR(32)  NOT NULL,
    url             VARCHAR(512) NOT NULL,
    branch          VARCHAR(128) NOT NULL DEFAULT 'main',
    local_path      VARCHAR(512) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    auth_type       VARCHAR(32),
    auth_token      VARCHAR(512),
    auth_username   VARCHAR(128),
    auth_password   VARCHAR(512),
    auth_ssh_key_path VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_repository_config_name UNIQUE (name),
    CONSTRAINT chk_repository_config_type CHECK (type IN ('GIT', 'GITHUB', 'GITLAB', 'GITEE', 'BITBUCKET', 'LOCAL')),
    CONSTRAINT chk_repository_config_auth_type CHECK (auth_type IN ('NONE', 'TOKEN', 'SSH_KEY', 'USERNAME_PASSWORD'))
);

CREATE INDEX IF NOT EXISTS idx_repository_config_enabled ON repository_config(enabled);

CREATE TABLE IF NOT EXISTS repository_sync_state (
    id              VARCHAR(64) PRIMARY KEY,
    repository_id   VARCHAR(64)  NOT NULL REFERENCES repository_config(id) ON DELETE CASCADE,
    status          VARCHAR(32)  NOT NULL,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    latest_commit   VARCHAR(64),
    previous_commit VARCHAR(64),
    changed_files   INTEGER      DEFAULT 0,
    error_message   TEXT,
    trigger_type    VARCHAR(32)  NOT NULL,
    triggered_by    VARCHAR(128),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_repository_sync_state_status CHECK (status IN ('PENDING', 'SYNCING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_repository_sync_state_trigger_type CHECK (trigger_type IN ('INITIAL', 'SCHEDULED', 'MANUAL', 'WEBHOOK'))
);

CREATE INDEX IF NOT EXISTS idx_repository_sync_state_repository_id ON repository_sync_state(repository_id);
CREATE INDEX IF NOT EXISTS idx_repository_sync_state_status ON repository_sync_state(status);
CREATE INDEX IF NOT EXISTS idx_repository_sync_state_created_at ON repository_sync_state(created_at);
