ALTER TABLE index_snapshot
    ADD COLUMN IF NOT EXISTS qdrant_snapshot_path VARCHAR(512),
    ADD COLUMN IF NOT EXISTS neo4j_backup_path   VARCHAR(512);
