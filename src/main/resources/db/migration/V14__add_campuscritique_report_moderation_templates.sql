INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000012', 'campuscritique', 'community.post_reported', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000013', 'campuscritique', 'community.comment_reported', 'IN_APP'),
    ('22222222-2222-2222-2222-000000000014', 'campuscritique', 'community.moderation_action_taken', 'IN_APP')
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
        '33333333-3333-3333-3333-000000000013',
        'campuscritique',
        'community.post_reported',
        'IN_APP',
        'campuscritique_community_post_reported_in_app_v1',
        'Discussion reported',
        '{{reporterName}} reported a discussion: {{discussionTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000014',
        'campuscritique',
        'community.comment_reported',
        'IN_APP',
        'campuscritique_community_comment_reported_in_app_v1',
        'Comment reported',
        '{{reporterName}} reported a comment in: {{discussionTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000015',
        'campuscritique',
        'community.moderation_action_taken',
        'IN_APP',
        'campuscritique_community_moderation_action_taken_in_app_v1',
        'Community content moderated',
        'Your {{targetType}} in {{discussionTitle}} was {{action}} by CampusCritique moderation.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
