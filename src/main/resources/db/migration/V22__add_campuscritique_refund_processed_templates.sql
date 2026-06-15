INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000047', 'campuscritique', 'connect.refund_processed_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000048', 'campuscritique', 'connect.refund_processed_student', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000048',
        'campuscritique',
        'connect.refund_processed_student',
        'IN_APP',
        'campuscritique_connect_refund_processed_student_in_app_v1',
        'Refund processed',
        'Your refund of {{refundAmount}} for the session with {{mentorName}} has been processed.'
    ),
    (
        '33333333-3333-3333-3333-000000000049',
        'campuscritique',
        'connect.refund_processed_student',
        'EMAIL',
        'campuscritique_connect_refund_processed_student_email_v1',
        'Your CampusCritique Connect refund has been processed',
        'Hi {{studentName}},\n\nYour refund of {{refundAmount}} for the Connect session with {{mentorName}} on {{sessionDate}} has been processed successfully.\n\nRefund details:\n- Session: {{sessionDate}} at {{sessionTime}}\n- Mentor: {{mentorName}}\n- Refund amount: {{refundAmount}}\n- Refund ID: {{refundId}}\n\nThe amount should reflect in your original payment method within 5-10 business days.\n\nThanks,\nCampusCritique Team'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();