CREATE TABLE notification_delivery_attempts (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    tenant_slug VARCHAR(80) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    provider_message_id VARCHAR(240),
    error_message VARCHAR(1000),
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_delivery_attempts_channel_check CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP')),
    CONSTRAINT notification_delivery_attempts_status_check CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT fk_notification_delivery_attempts_job_id FOREIGN KEY (job_id) REFERENCES notification_jobs (id),
    CONSTRAINT fk_notification_delivery_attempts_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE INDEX idx_notification_delivery_attempts_job_attempt
    ON notification_delivery_attempts (job_id, attempt_number);

CREATE INDEX idx_notification_delivery_attempts_tenant_channel_attempted
    ON notification_delivery_attempts (tenant_slug, channel, attempted_at DESC);
