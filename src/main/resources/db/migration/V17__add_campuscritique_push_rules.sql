INSERT INTO notification_rules (id, tenant_slug, event_type, channel)
VALUES
    ('22222222-2222-2222-2222-000000000016', 'campuscritique', 'review.submitted', 'PUSH'),
    ('22222222-2222-2222-2222-000000000017', 'campuscritique', 'review.approved', 'PUSH'),
    ('22222222-2222-2222-2222-000000000018', 'campuscritique', 'review.rejected', 'PUSH'),
    ('22222222-2222-2222-2222-000000000019', 'campuscritique', 'community.discussion_created', 'PUSH'),
    ('22222222-2222-2222-2222-000000000020', 'campuscritique', 'community.discussion_replied', 'PUSH'),
    ('22222222-2222-2222-2222-000000000021', 'campuscritique', 'community.comment_replied', 'PUSH'),
    ('22222222-2222-2222-2222-000000000022', 'campuscritique', 'community.post_reported', 'PUSH'),
    ('22222222-2222-2222-2222-000000000023', 'campuscritique', 'community.comment_reported', 'PUSH'),
    ('22222222-2222-2222-2222-000000000024', 'campuscritique', 'community.moderation_action_taken', 'PUSH')
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
        '33333333-3333-3333-3333-000000000017',
        'campuscritique',
        'review.submitted',
        'PUSH',
        'campuscritique_review_submitted_push_v1',
        'New review submitted',
        '{{reviewerName}} submitted a review for {{collegeName}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000018',
        'campuscritique',
        'review.approved',
        'PUSH',
        'campuscritique_review_approved_push_v1',
        'Review approved',
        'Your review for {{collegeName}} is now live.'
    ),
    (
        '33333333-3333-3333-3333-000000000019',
        'campuscritique',
        'review.rejected',
        'PUSH',
        'campuscritique_review_rejected_push_v1',
        'Review update',
        'Your review for {{collegeName}} was not approved.'
    ),
    (
        '33333333-3333-3333-3333-000000000020',
        'campuscritique',
        'community.discussion_created',
        'PUSH',
        'campuscritique_community_discussion_created_push_v1',
        'New discussion',
        '{{authorName}} started: {{discussionTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000021',
        'campuscritique',
        'community.discussion_replied',
        'PUSH',
        'campuscritique_community_discussion_replied_push_v1',
        'New discussion reply',
        '{{actorName}} replied to your discussion.'
    ),
    (
        '33333333-3333-3333-3333-000000000022',
        'campuscritique',
        'community.comment_replied',
        'PUSH',
        'campuscritique_community_comment_replied_push_v1',
        'New comment reply',
        '{{actorName}} replied to your comment.'
    ),
    (
        '33333333-3333-3333-3333-000000000023',
        'campuscritique',
        'community.post_reported',
        'PUSH',
        'campuscritique_community_post_reported_push_v1',
        'Discussion reported',
        '{{reporterName}} reported: {{discussionTitle}}.'
    ),
    (
        '33333333-3333-3333-3333-000000000024',
        'campuscritique',
        'community.comment_reported',
        'PUSH',
        'campuscritique_community_comment_reported_push_v1',
        'Comment reported',
        '{{reporterName}} reported a comment.'
    ),
    (
        '33333333-3333-3333-3333-000000000025',
        'campuscritique',
        'community.moderation_action_taken',
        'PUSH',
        'campuscritique_community_moderation_action_taken_push_v1',
        'Community content moderated',
        'Your {{targetType}} was {{action}}.'
    )
ON CONFLICT (tenant_slug, template_key) DO UPDATE
SET event_type = EXCLUDED.event_type,
    channel = EXCLUDED.channel,
    subject_template = EXCLUDED.subject_template,
    body_template = EXCLUDED.body_template,
    enabled = TRUE,
    updated_at = NOW();
