INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000006', 'campuscritique', 'review.submitted', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000007', 'campuscritique', 'review.approved', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000008', 'campuscritique', 'review.rejected', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000009', 'campuscritique', 'review.rejected', 'EMAIL'),
    ('22222222-2222-2222-2222-000000000010', 'campuscritique', 'community.discussion_replied', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000011', 'campuscritique', 'community.comment_replied', 'IN_APP')
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
        '33333333-3333-3333-3333-000000000006',
        'campuscritique',
        'review.submitted',
        'IN_APP',
        'campuscritique_review_submitted_in_app_v1',
        'New review submitted',
        'New review submitted for {{collegeName}} by {{reviewerName}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000007',
        'campuscritique',
        'review.approved',
        'IN_APP',
        'campuscritique_review_approved_in_app_v2',
        'Review approved',
        'Your review for {{collegeName}} has been approved and is now live.'
    ),
    (
        '33333333-3333-3333-3333-000000000008',
        'campuscritique',
        'review.approved',
        'EMAIL',
        'campuscritique_review_approved_email_v1',
        'Your CampusCritique review is live',
        'Your review for {{collegeName}} has been approved and is now live.'
    ),
    (
        '33333333-3333-3333-3333-000000000009',
        'campuscritique',
        'review.rejected',
        'IN_APP',
        'campuscritique_review_rejected_in_app_v1',
        'Review update',
        'Your review for {{collegeName}} was not approved. You can review the guidelines and submit again.'
    ),
    (
        '33333333-3333-3333-3333-000000000010',
        'campuscritique',
        'review.rejected',
        'EMAIL',
        'campuscritique_review_rejected_email_v1',
        'Update on your CampusCritique review',
        'Your review for {{collegeName}} was not approved. You can review the guidelines and submit again.'
    ),
    (
        '33333333-3333-3333-3333-000000000011',
        'campuscritique',
        'community.discussion_replied',
        'IN_APP',
        'campuscritique_community_discussion_replied_in_app_v1',
        'New discussion reply',
        '{{actorName}} replied to your discussion: {{discussionTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000012',
        'campuscritique',
        'community.comment_replied',
        'IN_APP',
        'campuscritique_community_comment_replied_in_app_v1',
        'New comment reply',
        '{{actorName}} replied to your comment in: {{discussionTitle}}.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
