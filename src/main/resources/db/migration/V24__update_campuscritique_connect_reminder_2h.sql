-- Remove old 24h and 10min reminder rules (they will be replaced by 2h rules)
DELETE FROM notification_rules
WHERE tenant_slug = 'campuscritique'
  AND event_type IN ('connect.session_reminder_24h', 'connect.session_reminder_10min');

-- Disable old 24h and 10min templates (keep them in DB for history, but disabled)
UPDATE notification_templates
SET enabled = FALSE,
    updated_at = NOW()
WHERE tenant_slug = 'campuscritique'
  AND template_key IN (
      'campuscritique_connect_session_reminder_24h_in_app_v1',
      'campuscritique_connect_session_reminder_24h_email_v1',
      'campuscritique_connect_session_reminder_10min_in_app_v1'
  );

-- Add new 2h reminder rules
INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000051', 'campuscritique', 'connect.session_reminder_2h', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000052', 'campuscritique', 'connect.session_reminder_2h', 'EMAIL')
ON CONFLICT (tenant_slug, event_type, channel) DO UPDATE
SET enabled = TRUE,
    updated_at = NOW();

-- Add new 2h reminder templates
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
        '33333333-3333-3333-3333-000000000052',
        'campuscritique',
        'connect.session_reminder_2h',
        'IN_APP',
        'campuscritique_connect_session_reminder_2h_in_app_v1',
        'Connect session starting soon',
        'Reminder: your Connect session with {{counterpartyName}} starts at {{sessionTime}} today.'
    ),
    (
        '33333333-3333-3333-3333-000000000053',
        'campuscritique',
        'connect.session_reminder_2h',
        'EMAIL',
        'campuscritique_connect_session_reminder_2h_email_v1',
        'Reminder: your CampusCritique Connect session is coming up',
        'Your CampusCritique Connect session with {{counterpartyName}} is coming up at {{sessionTime}} today. Join from your dashboard when it is time.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
