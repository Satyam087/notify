CREATE TABLE notification_events (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    recipient JSONB NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_events_status_check CHECK (status IN ('RECEIVED', 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_notification_events_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE UNIQUE INDEX ux_notification_events_tenant_idempotency
    ON notification_events (tenant_slug, idempotency_key);

CREATE INDEX idx_notification_events_tenant_type
    ON notification_events (tenant_slug, event_type);

CREATE INDEX idx_notification_events_status_created
    ON notification_events (status, created_at);

INSERT INTO tenants (id, slug, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'campuscritique', 'CampusCritique')
ON CONFLICT (slug) DO NOTHING;
