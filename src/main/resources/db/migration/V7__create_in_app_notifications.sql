CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    job_id UUID NOT NULL,
    event_id UUID NOT NULL,
    recipient_user_id VARCHAR(160) NOT NULL,
    title VARCHAR(240) NOT NULL,
    body TEXT NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_in_app_notifications_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug),
    CONSTRAINT fk_in_app_notifications_job_id FOREIGN KEY (job_id) REFERENCES notification_jobs (id),
    CONSTRAINT fk_in_app_notifications_event_id FOREIGN KEY (event_id) REFERENCES notification_events (id)
);

CREATE UNIQUE INDEX ux_in_app_notifications_job
    ON in_app_notifications (job_id);

CREATE INDEX idx_in_app_notifications_tenant_recipient_created
    ON in_app_notifications (tenant_slug, recipient_user_id, created_at DESC);

CREATE INDEX idx_in_app_notifications_tenant_recipient_unread
    ON in_app_notifications (tenant_slug, recipient_user_id, read_at)
    WHERE read_at IS NULL;
