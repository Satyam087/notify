CREATE TABLE notification_templates (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    template_key VARCHAR(160) NOT NULL,
    subject_template VARCHAR(240) NOT NULL,
    body_template TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_templates_channel_check CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'IN_APP')),
    CONSTRAINT fk_notification_templates_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE UNIQUE INDEX ux_notification_templates_tenant_key
    ON notification_templates (tenant_slug, template_key);

CREATE INDEX idx_notification_templates_tenant_event_channel_enabled
    ON notification_templates (tenant_slug, event_type, channel, enabled);

INSERT INTO notification_templates (
    id,
    tenant_slug,
    event_type,
    channel,
    template_key,
    subject_template,
    body_template
)
VALUES
    (
        '33333333-3333-3333-3333-000000000001',
        'campuscritique',
        'connect.requested',
        'IN_APP',
        'campuscritique_connect_requested_in_app_v1',
        'New connect request',
        'A student requested a connect for {{collegeName}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000002',
        'campuscritique',
        'connect.requested',
        'EMAIL',
        'campuscritique_connect_requested_email_v1',
        'New connect request for {{collegeName}}',
        'A student requested a connect for {{collegeName}}. Open CampusCritique to review the request.'
    ),
    (
        '33333333-3333-3333-3333-000000000003',
        'campuscritique',
        'review.approved',
        'IN_APP',
        'campuscritique_review_approved_in_app_v1',
        'Review approved',
        'Your review for {{collegeName}} is now live.'
    ),
    (
        '33333333-3333-3333-3333-000000000004',
        'campuscritique',
        'community.reply.created',
        'IN_APP',
        'campuscritique_community_reply_created_in_app_v1',
        'New reply',
        'Someone replied to your post: {{postTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000005',
        'campuscritique',
        'lead.created',
        'EMAIL',
        'campuscritique_lead_created_email_v1',
        'New CampusCritique lead',
        'New lead from {{name}} for {{interest}}.'
    )
ON CONFLICT (tenant_slug, template_key) DO NOTHING;

ALTER TABLE notification_jobs
    ADD COLUMN template_id UUID;

ALTER TABLE notification_jobs
    ADD CONSTRAINT fk_notification_jobs_template_id
    FOREIGN KEY (template_id) REFERENCES notification_templates (id);

CREATE INDEX idx_notification_jobs_template_id
    ON notification_jobs (template_id);
