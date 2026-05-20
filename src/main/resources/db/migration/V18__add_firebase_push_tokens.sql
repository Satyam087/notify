ALTER TABLE push_subscriptions
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'WEB_PUSH',
    ADD COLUMN fcm_token TEXT;

ALTER TABLE push_subscriptions
    ALTER COLUMN p256dh_key DROP NOT NULL,
    ALTER COLUMN auth_key DROP NOT NULL;

CREATE UNIQUE INDEX ux_push_subscriptions_fcm_token
    ON push_subscriptions (fcm_token)
    WHERE fcm_token IS NOT NULL;

CREATE INDEX idx_push_subscriptions_provider_active
    ON push_subscriptions (provider, active);
