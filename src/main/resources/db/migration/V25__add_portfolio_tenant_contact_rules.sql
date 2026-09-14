-- Portfolio tenant: satyamkumarsingh.com contact form runs on Notify.
-- The API key is created out of band with `admin:create-api-key` (never in a migration).
INSERT INTO tenants (id, slug, name)
VALUES ('44444444-4444-4444-4444-444444444444', 'portfolio', 'Satyam Kumar Singh Portfolio')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000101', 'portfolio', 'portfolio.contact', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000102', 'portfolio', 'portfolio.contact', 'EMAIL')
ON CONFLICT (tenant_slug, event_type, channel) DO UPDATE
SET enabled = TRUE,
    updated_at = NOW();

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
        '33333333-3333-3333-3333-000000000101',
        'portfolio',
        'portfolio.contact',
        'IN_APP',
        'portfolio_contact_in_app_v1',
        'Message from {{name}}',
        '{{name}} ({{email}}) wrote via satyamkumarsingh.com: {{message}}'
    ),
    (
        '33333333-3333-3333-3333-000000000102',
        'portfolio',
        'portfolio.contact',
        'EMAIL',
        'portfolio_contact_email_v1',
        'satyamkumarsingh.com: message from {{name}}',
        'New message from the portfolio contact form.

From: {{name}} <{{email}}>
Page: {{page}}

{{message}}

Reply directly to {{email}}. Trace id: {{traceId}}'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
