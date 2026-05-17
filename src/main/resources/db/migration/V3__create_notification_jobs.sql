CREATE TABLE notification_jobs (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    tenant_slug VARCHAR(80) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_jobs_channel_check CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP')),
    CONSTRAINT notification_jobs_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_notification_jobs_event_id FOREIGN KEY (event_id) REFERENCES notification_events (id),
    CONSTRAINT fk_notification_jobs_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE UNIQUE INDEX ux_notification_jobs_event_channel
    ON notification_jobs (event_id, channel);

CREATE INDEX idx_notification_jobs_status_next_attempt
    ON notification_jobs (status, next_attempt_at, created_at);

CREATE INDEX idx_notification_jobs_tenant_channel
    ON notification_jobs (tenant_slug, channel, created_at);
