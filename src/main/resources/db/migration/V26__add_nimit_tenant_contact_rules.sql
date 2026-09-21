-- Third tenant: the contact form on Nimit Jain's portfolio runs on Notify.
-- The API key is created out of band with `admin:create-api-key --tenant=nimit` (never in a migration).
INSERT INTO tenants (id, slug, name)
VALUES ('55555555-5555-5555-5555-555555555555', 'nimit', 'Nimit Jain Portfolio')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000201', 'nimit', 'nimit.contact', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000202', 'nimit', 'nimit.contact', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000201',
        'nimit',
        'nimit.contact',
        'IN_APP',
        'nimit_contact_in_app_v1',
        'Message from {{name}}',
        '{{name}} ({{email}}) wrote via his portfolio site: {{message}}'
    ),
    (
        '33333333-3333-3333-3333-000000000202',
        'nimit',
        'nimit.contact',
        'EMAIL',
        'nimit_contact_email_v1',
        'Portfolio: message from {{name}}',
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
