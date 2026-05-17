CREATE TABLE tenant_api_keys (
    id UUID PRIMARY KEY,
    tenant_slug VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash CHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT tenant_api_keys_status_check CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT fk_tenant_api_keys_tenant_slug FOREIGN KEY (tenant_slug) REFERENCES tenants (slug)
);

CREATE INDEX idx_tenant_api_keys_tenant_status
    ON tenant_api_keys (tenant_slug, status);

CREATE INDEX idx_tenant_api_keys_prefix
    ON tenant_api_keys (key_prefix);
