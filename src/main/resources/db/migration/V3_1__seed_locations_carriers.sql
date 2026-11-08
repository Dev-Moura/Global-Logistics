INSERT INTO tb_locations (id, name, city, state, country_code, latitude, longitude) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Shanghai Port', 'Shanghai', 'Shanghai', 'CN', 31.2304, 121.4737),
    ('22222222-2222-2222-2222-222222222222', 'Rotterdam Port', 'Rotterdam', 'South Holland', 'NL', 51.9244, 4.4777),
    ('33333333-3333-3333-3333-333333333333', 'Santos Port', 'Santos', 'Sao Paulo', 'BR', -23.9608, -46.3336),
    ('44444444-4444-4444-4444-444444444444', 'Los Angeles Port', 'Los Angeles', 'California', 'US', 33.7405, -118.2775)
ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_carriers (id, name, country_code, contact_information, integration_type, status) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Maersk', 'DK', 'ops@maersk.example', 'API', 'ACTIVE'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'DHL', 'DE', 'ops@dhl.example', 'API', 'ACTIVE'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'FedEx', 'US', 'ops@fedex.example', 'EDI', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
