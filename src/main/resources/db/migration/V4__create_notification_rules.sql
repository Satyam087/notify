CREATE TABLE notification_rules (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_rules_channel_check CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP')),
    CONSTRAINT fk_notification_rules_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE UNIQUE INDEX ux_notification_rules_tenant_event_channel
    ON notification_rules (tenant_slug, event_type, channel);

CREATE INDEX idx_notification_rules_tenant_event_enabled
    ON notification_rules (tenant_slug, event_type, enabled);

INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000001', 'campuscritique', 'connect.requested', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000002', 'campuscritique', 'connect.requested', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000003', 'campuscritique', 'review.approved', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000004', 'campuscritique', 'community.reply.created', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000005', 'campuscritique', 'lead.created', 'EMAIL')
ON CONFLICT (tenant_slug, event_type, channel) DO NOTHING;
