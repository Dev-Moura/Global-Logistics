CREATE TABLE tb_tracking_events (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES tb_shipments (id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    description VARCHAR(1000),
    location_id UUID REFERENCES tb_locations (id),
    event_timestamp TIMESTAMPTZ NOT NULL,
    source VARCHAR(32) NOT NULL,
    external_event_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_tb_tracking_events_type CHECK (event_type IN (
        'PICKED_UP',
        'DEPARTED',
        'IN_TRANSIT',
        'ARRIVED_AT_FACILITY',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'CUSTOMS_CLEARED',
        'CUSTOMS_HOLD',
        'EXCEPTION',
        'CANCELLED',
        'OTHER'
    )),
    CONSTRAINT ck_tb_tracking_events_source CHECK (source IN (
        'MANUAL',
        'EXTERNAL',
        'SYSTEM'
    ))
);

CREATE UNIQUE INDEX ux_tracking_events_shipment_external
    ON tb_tracking_events (shipment_id, external_event_id)
    WHERE external_event_id IS NOT NULL;

CREATE INDEX ix_tracking_events_shipment_ts
    ON tb_tracking_events (shipment_id, event_timestamp);

CREATE INDEX ix_tracking_events_shipment_created
    ON tb_tracking_events (shipment_id, created_at);
