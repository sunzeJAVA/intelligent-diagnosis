CREATE TABLE IF NOT EXISTS app_user (
    username   VARCHAR(64)  PRIMARY KEY,
    password   VARCHAR(256) NOT NULL,
    role       VARCHAR(32)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_user_role ON app_user(role);
