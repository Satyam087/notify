ALTER TABLE notification_jobs
    ADD COLUMN rendered_subject VARCHAR(240),
    ADD COLUMN rendered_body TEXT;

UPDATE notification_jobs
SET
    rendered_subject = '',
    rendered_body = ''
WHERE rendered_subject IS NULL
   OR rendered_body IS NULL;

ALTER TABLE notification_jobs
    ALTER COLUMN rendered_subject SET NOT NULL,
    ALTER COLUMN rendered_body SET NOT NULL;
