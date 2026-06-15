INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000049', 'campuscritique', 'connect.refund_failed_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000050', 'campuscritique', 'connect.refund_failed_student', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000050',
        'campuscritique',
        'connect.refund_failed_student',
        'IN_APP',
        'campuscritique_connect_refund_failed_student_in_app_v1',
        'Refund could not be processed',
        'We were unable to process your refund of {{refundAmount}} for the session with {{mentorName}}. Please contact support.'
    ),
    (
        '33333333-3333-3333-3333-000000000051',
        'campuscritique',
        'connect.refund_failed_student',
        'EMAIL',
        'campuscritique_connect_refund_failed_student_email_v1',
        'Your CampusCritique Connect refund could not be processed',
        'Hi {{studentName}},\n\nWe attempted to process your refund of {{refundAmount}} for the Connect session with {{mentorName}} on {{sessionDate}}, but it could not be completed.\n\nRefund details:\n- Session: {{sessionDate}} at {{sessionTime}}\n- Mentor: {{mentorName}}\n- Refund amount: {{refundAmount}}\n- Refund ID: {{refundId}}\n- Failure reason: {{refundFailureReason}}\n\nPlease contact our support team at connect@campuscritique.in to resolve this. We apologize for the inconvenience.\n\nThanks,\nCampusCritique Team'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();