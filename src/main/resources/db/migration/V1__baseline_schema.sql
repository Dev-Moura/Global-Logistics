CREATE TABLE tb_shipments (
    id UUID PRIMARY KEY,
    tracking_number VARCHAR(255) NOT NULL UNIQUE,
    origin_country VARCHAR(255) NOT NULL,
    destination_country VARCHAR(255) NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    insurance_value NUMERIC(38, 2) NOT NULL,
    status VARCHAR(255),
    created_at TIMESTAMP,
    distance DOUBLE PRECISION,
    shipping_cost NUMERIC(38, 2)
);
