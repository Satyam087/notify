INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000045', 'campuscritique', 'connect.review_ready_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000046', 'campuscritique', 'connect.review_ready_student', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000046',
        'campuscritique',
        'connect.review_ready_student',
        'IN_APP',
        'campuscritique_connect_review_ready_student_in_app_v1',
        'How was your session with {{mentorName}}?',
        'How was your session with {{mentorName}}? Leave a review.'
    ),
    (
        '33333333-3333-3333-3333-000000000047',
        'campuscritique',
        'connect.review_ready_student',
        'EMAIL',
        'campuscritique_connect_review_ready_student_email_v1',
        'How was your Connect session with {{mentorName}}?',
        'Hi {{studentName}},\n\nHow was your Connect session with {{mentorName}} on {{sessionDate}}?\n\nYour feedback helps other students make informed decisions.\n\n<a href="{{deepLink}}" style="display: inline-block; padding: 12px 24px; background-color: #2563EB; color: white; text-decoration: none; border-radius: 6px; font-weight: 600;">Review your mentor</a>\n\nThanks,\nCampusCritique Team'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();