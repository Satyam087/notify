ALTER TABLE in_app_notifications
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE in_app_notifications
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE in_app_notifications
    ALTER COLUMN updated_at SET NOT NULL;
