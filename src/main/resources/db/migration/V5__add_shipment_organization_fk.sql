ALTER TABLE tb_shipments
    ADD CONSTRAINT fk_shipments_organization
    FOREIGN KEY (organization_id) REFERENCES tb_organizations (id);
