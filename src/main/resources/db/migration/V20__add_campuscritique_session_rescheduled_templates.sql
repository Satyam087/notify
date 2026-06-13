INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000041', 'campuscritique', 'connect.session_rescheduled_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000042', 'campuscritique', 'connect.session_rescheduled_student', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000043', 'campuscritique', 'connect.session_rescheduled_mentor', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000044', 'campuscritique', 'connect.session_rescheduled_mentor', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000042',
        'campuscritique',
        'connect.session_rescheduled_student',
        'IN_APP',
        'campuscritique_connect_session_rescheduled_student_in_app_v1',
        'Connect session rescheduled',
        'Your session with {{mentorName}} has been rescheduled to {{newSessionDate}} at {{newSessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000043',
        'campuscritique',
        'connect.session_rescheduled_student',
        'EMAIL',
        'campuscritique_connect_session_rescheduled_student_email_v1',
        'Your CampusCritique Connect session has been rescheduled',
        'Hi {{studentName}}, your {{durationMinutes}}-minute Connect session with {{mentorName}} has been rescheduled from {{oldSessionDate}} at {{oldSessionTime}} to {{newSessionDate}} at {{newSessionTime}}. You can view details or join the meeting here: {{deepLink}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000044',
        'campuscritique',
        'connect.session_rescheduled_mentor',
        'IN_APP',
        'campuscritique_connect_session_rescheduled_mentor_in_app_v1',
        'Connect session rescheduled',
        'Your session with {{studentName}} has been rescheduled to {{newSessionDate}} at {{newSessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000045',
        'campuscritique',
        'connect.session_rescheduled_mentor',
        'EMAIL',
        'campuscritique_connect_session_rescheduled_mentor_email_v1',
        'Your CampusCritique Connect session has been rescheduled',
        'Hi {{mentorName}}, your {{durationMinutes}}-minute Connect session with {{studentName}} has been rescheduled from {{oldSessionDate}} at {{oldSessionTime}} to {{newSessionDate}} at {{newSessionTime}}. You can view details here: {{deepLink}}.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
