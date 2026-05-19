CREATE TABLE push_subscriptions (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    recipient_user_id VARCHAR(180) NOT NULL,
    endpoint TEXT NOT NULL,
    p256dh_key TEXT NOT NULL,
    auth_key TEXT NOT NULL,
    user_agent TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    last_success_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_push_subscriptions_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE UNIQUE INDEX ux_push_subscriptions_endpoint
    ON push_subscriptions (endpoint);

CREATE INDEX idx_push_subscriptions_tenant_user_active
    ON push_subscriptions (tenant_slug, recipient_user_id, active);
