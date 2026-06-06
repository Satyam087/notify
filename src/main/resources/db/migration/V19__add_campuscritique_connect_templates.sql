INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000025', 'campuscritique', 'connect.mentor_access_granted', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000026', 'campuscritique', 'connect.mentor_access_granted', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000027', 'campuscritique', 'connect.session_booked_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000028', 'campuscritique', 'connect.session_booked_student', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000029', 'campuscritique', 'connect.session_booked_mentor', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000030', 'campuscritique', 'connect.session_booked_mentor', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000031', 'campuscritique', 'connect.session_reminder_24h', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000032', 'campuscritique', 'connect.session_reminder_24h', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000033', 'campuscritique', 'connect.session_reminder_10min', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000034', 'campuscritique', 'connect.session_cancelled_student', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000035', 'campuscritique', 'connect.session_cancelled_student', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000036', 'campuscritique', 'connect.session_cancelled_mentor', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000037', 'campuscritique', 'connect.session_cancelled_mentor', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000038', 'campuscritique', 'connect.review_submitted', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000039', 'campuscritique', 'connect.payout_processed', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000040', 'campuscritique', 'connect.payout_processed', 'EMAIL')
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
        '33333333-3333-3333-3333-000000000026',
        'campuscritique',
        'connect.mentor_access_granted',
        'IN_APP',
        'campuscritique_connect_mentor_access_granted_in_app_v1',
        'Mentor access granted',
        'You can now complete your CampusCritique Connect mentor profile for {{collegeName}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000027',
        'campuscritique',
        'connect.mentor_access_granted',
        'EMAIL',
        'campuscritique_connect_mentor_access_granted_email_v1',
        'You are now a CampusCritique Connect mentor',
        'Hi {{mentorName}}, you can now complete your CampusCritique Connect mentor profile for {{collegeName}} and go live when your profile is ready.'
    ),
    (
        '33333333-3333-3333-3333-000000000028',
        'campuscritique',
        'connect.session_booked_student',
        'IN_APP',
        'campuscritique_connect_session_booked_student_in_app_v1',
        'Connect session confirmed',
        'Your session with {{mentorName}} is confirmed for {{sessionDate}} at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000029',
        'campuscritique',
        'connect.session_booked_student',
        'EMAIL',
        'campuscritique_connect_session_booked_student_email_v1',
        'Your CampusCritique Connect session is confirmed',
        'Your session with {{mentorName}} is confirmed for {{sessionDate}} at {{sessionTime}}. You can join it from your CampusCritique dashboard.'
    ),
    (
        '33333333-3333-3333-3333-000000000030',
        'campuscritique',
        'connect.session_booked_mentor',
        'IN_APP',
        'campuscritique_connect_session_booked_mentor_in_app_v1',
        'New Connect session booked',
        '{{studentName}} booked a {{durationMinutes}}-minute session for {{sessionDate}} at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000031',
        'campuscritique',
        'connect.session_booked_mentor',
        'EMAIL',
        'campuscritique_connect_session_booked_mentor_email_v1',
        'New CampusCritique Connect session booked',
        '{{studentName}} booked a {{durationMinutes}}-minute session with you for {{sessionDate}} at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000032',
        'campuscritique',
        'connect.session_reminder_24h',
        'IN_APP',
        'campuscritique_connect_session_reminder_24h_in_app_v1',
        'Connect session tomorrow',
        'Reminder: your Connect session with {{counterpartyName}} is tomorrow at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000033',
        'campuscritique',
        'connect.session_reminder_24h',
        'EMAIL',
        'campuscritique_connect_session_reminder_24h_email_v1',
        'Reminder: your Connect session is tomorrow',
        'Your CampusCritique Connect session with {{counterpartyName}} is tomorrow at {{sessionTime}}. Join from your dashboard when it is time.'
    ),
    (
        '33333333-3333-3333-3333-000000000034',
        'campuscritique',
        'connect.session_reminder_10min',
        'IN_APP',
        'campuscritique_connect_session_reminder_10min_in_app_v1',
        'Connect session starting soon',
        'Your Connect session with {{counterpartyName}} starts in about 10 minutes.'
    ),
    (
        '33333333-3333-3333-3333-000000000035',
        'campuscritique',
        'connect.session_cancelled_student',
        'IN_APP',
        'campuscritique_connect_session_cancelled_student_in_app_v1',
        'Connect session cancelled',
        'Your Connect session with {{mentorName}} was cancelled. {{refundMessage}}'
    ),
    (
        '33333333-3333-3333-3333-000000000036',
        'campuscritique',
        'connect.session_cancelled_student',
        'EMAIL',
        'campuscritique_connect_session_cancelled_student_email_v1',
        'Your CampusCritique Connect session was cancelled',
        'Your Connect session with {{mentorName}} was cancelled. {{refundMessage}}'
    ),
    (
        '33333333-3333-3333-3333-000000000037',
        'campuscritique',
        'connect.session_cancelled_mentor',
        'IN_APP',
        'campuscritique_connect_session_cancelled_mentor_in_app_v1',
        'Connect session cancelled',
        '{{studentName}} cancelled the Connect session scheduled for {{sessionDate}} at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000038',
        'campuscritique',
        'connect.session_cancelled_mentor',
        'EMAIL',
        'campuscritique_connect_session_cancelled_mentor_email_v1',
        'A CampusCritique Connect session was cancelled',
        '{{studentName}} cancelled the Connect session scheduled for {{sessionDate}} at {{sessionTime}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000039',
        'campuscritique',
        'connect.review_submitted',
        'IN_APP',
        'campuscritique_connect_review_submitted_in_app_v1',
        'New Connect review',
        '{{studentName}} rated your Connect session {{rating}}/5.'
    ),
    (
        '33333333-3333-3333-3333-000000000040',
        'campuscritique',
        'connect.payout_processed',
        'IN_APP',
        'campuscritique_connect_payout_processed_in_app_v1',
        'Connect payout processed',
        'Your Connect payout of {{payoutAmount}} has been marked as paid.'
    ),
    (
        '33333333-3333-3333-3333-000000000041',
        'campuscritique',
        'connect.payout_processed',
        'EMAIL',
        'campuscritique_connect_payout_processed_email_v1',
        'Your CampusCritique Connect payout has been processed',
        'Your Connect payout of {{payoutAmount}} has been marked as paid. Reference: {{referenceId}}.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
