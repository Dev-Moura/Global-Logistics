CREATE TABLE tb_locations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255),
    country_code VARCHAR(2) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE TABLE tb_carriers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    contact_information VARCHAR(255),
    integration_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT ck_tb_carriers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

ALTER TABLE tb_shipments
    ADD COLUMN organization_id UUID,
    ADD COLUMN origin_id UUID REFERENCES tb_locations (id),
    ADD COLUMN destination_id UUID REFERENCES tb_locations (id),
    ADD COLUMN carrier_id UUID REFERENCES tb_carriers (id),
    ADD COLUMN estimated_departure TIMESTAMPTZ,
    ADD COLUMN estimated_arrival TIMESTAMPTZ,
    ADD COLUMN actual_departure TIMESTAMPTZ,
    ADD COLUMN actual_arrival TIMESTAMPTZ,
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tb_shipments ALTER COLUMN origin_country DROP NOT NULL;
ALTER TABLE tb_shipments ALTER COLUMN destination_country DROP NOT NULL;
ALTER TABLE tb_shipments ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE tb_shipments ALTER COLUMN weight TYPE NUMERIC(38, 2) USING weight::NUMERIC(38, 2);
ALTER TABLE tb_shipments ALTER COLUMN distance TYPE NUMERIC(38, 2) USING distance::NUMERIC(38, 2);

ALTER TABLE tb_shipments DROP CONSTRAINT IF EXISTS tb_shipments_tracking_number_key;

CREATE UNIQUE INDEX ux_shipments_org_tracking ON tb_shipments (organization_id, tracking_number);
CREATE INDEX ix_shipments_org_status ON tb_shipments (organization_id, status);
CREATE INDEX ix_shipments_org_created ON tb_shipments (organization_id, created_at);
