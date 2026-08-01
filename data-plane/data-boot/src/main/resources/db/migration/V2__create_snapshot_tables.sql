CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS index_snapshot (
    id                  VARCHAR(64) PRIMARY KEY,
    repository_id       VARCHAR(64)  NOT NULL,
    repository_name     VARCHAR(128) NOT NULL,
    branch              VARCHAR(128),
    commit_hash         VARCHAR(64),
    previous_commit     VARCHAR(64),
    commit_message      TEXT,
    author              VARCHAR(128),
    status              VARCHAR(32)  NOT NULL,
    element_count       BIGINT       NOT NULL DEFAULT 0,
    relation_count      BIGINT       NOT NULL DEFAULT 0,
    before_snapshot_id  VARCHAR(64),
    after_snapshot_id   VARCHAR(64),
    checksum            VARCHAR(256),
    workflow_id         VARCHAR(128),
    element_ids         JSONB,
    created_at          TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ,

    CONSTRAINT chk_index_snapshot_status CHECK (status IN ('CREATING', 'VALIDATING', 'PROMOTED', 'FAILED', 'ROLLED_BACK'))
);

CREATE INDEX IF NOT EXISTS idx_index_snapshot_repository_name ON index_snapshot(repository_name);
CREATE INDEX IF NOT EXISTS idx_index_snapshot_status ON index_snapshot(status);
CREATE INDEX IF NOT EXISTS idx_index_snapshot_created_at ON index_snapshot(created_at);
CREATE INDEX IF NOT EXISTS idx_index_snapshot_workflow_id ON index_snapshot(workflow_id);

CREATE TABLE IF NOT EXISTS snapshot_validation (
    id              VARCHAR(64) PRIMARY KEY,
    snapshot_id     VARCHAR(64)  NOT NULL REFERENCES index_snapshot(id) ON DELETE CASCADE,
    validation_type VARCHAR(32)  NOT NULL,
    result          VARCHAR(32)  NOT NULL,
    details         TEXT,
    validated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_snapshot_validation_type CHECK (validation_type IN ('INTEGRITY', 'CONSISTENCY', 'PERFORMANCE', 'SECURITY')),
    CONSTRAINT chk_snapshot_validation_result CHECK (result IN ('PASSED', 'FAILED', 'WARNING'))
);

CREATE INDEX IF NOT EXISTS idx_snapshot_validation_snapshot_id ON snapshot_validation(snapshot_id);
