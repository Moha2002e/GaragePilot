--liquibase formatted sql

--changeset garagepilot:006-dashboard-planning-services dbms:postgresql
CREATE TABLE IF NOT EXISTS garage_services (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    name varchar(120) NOT NULL,
    category varchar(40) NOT NULL,
    description text,
    duration_minutes integer NOT NULL,
    price numeric(12, 2) NOT NULL,
    vat_rate numeric(5, 2) NOT NULL,
    color varchar(20) NOT NULL DEFAULT '#167A5B',
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_garage_services_name UNIQUE (garage_id, name),
    CONSTRAINT ck_garage_services_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_garage_services_price CHECK (price >= 0),
    CONSTRAINT ck_garage_services_vat CHECK (vat_rate >= 0)
);

CREATE INDEX IF NOT EXISTS idx_users_garage ON users (garage_id);
CREATE INDEX IF NOT EXISTS idx_clients_garage_active ON clients (garage_id, active);
CREATE INDEX IF NOT EXISTS idx_vehicles_garage_client ON vehicles (garage_id, client_id);
CREATE INDEX IF NOT EXISTS idx_appointments_garage_vehicle ON appointments (garage_id, vehicle_id);
CREATE INDEX IF NOT EXISTS idx_appointments_garage_status ON appointments (garage_id, status);
CREATE INDEX IF NOT EXISTS idx_work_orders_garage_vehicle ON work_orders (garage_id, vehicle_id);
CREATE INDEX IF NOT EXISTS idx_work_order_items_work_order ON work_order_items (work_order_id);
CREATE INDEX IF NOT EXISTS idx_quotes_garage_client ON quotes (garage_id, client_id);
CREATE INDEX IF NOT EXISTS idx_quote_items_quote ON quote_items (quote_id);
CREATE INDEX IF NOT EXISTS idx_invoices_garage_issue_date ON invoices (garage_id, issue_date);
CREATE INDEX IF NOT EXISTS idx_invoices_garage_client ON invoices (garage_id, client_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items (invoice_id);
CREATE INDEX IF NOT EXISTS idx_garage_services_garage_active ON garage_services (garage_id, active);

ALTER TABLE garage_services ENABLE ROW LEVEL SECURITY;

--rollback DROP TABLE IF EXISTS garage_services;
