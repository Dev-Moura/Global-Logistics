CREATE TABLE tb_organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    tax_identifier VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_tb_organizations_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX ux_tb_organizations_country_tax
    ON tb_organizations (country_code, tax_identifier)
    WHERE tax_identifier IS NOT NULL;

CREATE TABLE tb_users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES tb_organizations (id),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_tb_users_role CHECK (role IN (
        'SYSTEM_ADMIN',
        'LOGISTICS_MANAGER',
        'LOGISTICS_OPERATOR',
        'COMPLIANCE_ANALYST',
        'BUSINESS_ANALYST'
    ))
);

CREATE UNIQUE INDEX ux_tb_users_email_lower ON tb_users (LOWER(email));
CREATE INDEX ix_tb_users_organization_id ON tb_users (organization_id);

CREATE TABLE tb_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tb_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_tb_refresh_tokens_user_id ON tb_refresh_tokens (user_id);
CREATE INDEX ix_tb_refresh_tokens_expires_at ON tb_refresh_tokens (expires_at);
