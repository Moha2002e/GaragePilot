--liquibase formatted sql

--changeset garagepilot:004-create-operational-tables dbms:postgresql
CREATE TABLE IF NOT EXISTS appointments (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    client_id uuid NOT NULL REFERENCES clients(id),
    vehicle_id uuid NOT NULL REFERENCES vehicles(id),
    start_date_time timestamptz NOT NULL,
    end_date_time timestamptz NOT NULL,
    reason varchar(255) NOT NULL,
    notes text,
    status varchar(30) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS work_orders (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    client_id uuid NOT NULL REFERENCES clients(id),
    vehicle_id uuid NOT NULL REFERENCES vehicles(id),
    appointment_id uuid REFERENCES appointments(id),
    status varchar(40) NOT NULL,
    mileage_in bigint,
    mileage_out bigint,
    description text,
    technician_notes text,
    customer_notes text,
    started_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS work_order_items (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    work_order_id uuid NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    type varchar(20) NOT NULL,
    description varchar(255) NOT NULL,
    quantity numeric(12, 2) NOT NULL,
    unit_price numeric(12, 2) NOT NULL,
    vat_rate numeric(5, 2) NOT NULL,
    total_excluding_vat numeric(12, 2) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS quotes (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    client_id uuid NOT NULL REFERENCES clients(id),
    vehicle_id uuid NOT NULL REFERENCES vehicles(id),
    work_order_id uuid REFERENCES work_orders(id),
    quote_number varchar(50) NOT NULL,
    status varchar(30) NOT NULL,
    issue_date date NOT NULL,
    valid_until date,
    subtotal numeric(12, 2) NOT NULL,
    vat_amount numeric(12, 2) NOT NULL,
    total numeric(12, 2) NOT NULL,
    customer_comment text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_quotes_garage_number UNIQUE (garage_id, quote_number)
);

CREATE TABLE IF NOT EXISTS quote_items (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    quote_id uuid NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    type varchar(20) NOT NULL,
    description varchar(255) NOT NULL,
    quantity numeric(12, 2) NOT NULL,
    unit_price numeric(12, 2) NOT NULL,
    vat_rate numeric(5, 2) NOT NULL,
    total_excluding_vat numeric(12, 2) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    client_id uuid NOT NULL REFERENCES clients(id),
    vehicle_id uuid NOT NULL REFERENCES vehicles(id),
    work_order_id uuid REFERENCES work_orders(id),
    quote_id uuid REFERENCES quotes(id),
    invoice_number varchar(50) NOT NULL,
    status varchar(30) NOT NULL,
    issue_date date NOT NULL,
    due_date date,
    subtotal numeric(12, 2) NOT NULL,
    vat_amount numeric(12, 2) NOT NULL,
    total numeric(12, 2) NOT NULL,
    amount_paid numeric(12, 2) NOT NULL DEFAULT 0,
    balance_due numeric(12, 2) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_invoices_garage_number UNIQUE (garage_id, invoice_number)
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    invoice_id uuid NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    type varchar(20) NOT NULL,
    description varchar(255) NOT NULL,
    quantity numeric(12, 2) NOT NULL,
    unit_price numeric(12, 2) NOT NULL,
    vat_rate numeric(5, 2) NOT NULL,
    total_excluding_vat numeric(12, 2) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id uuid PRIMARY KEY,
    garage_id uuid NOT NULL REFERENCES garages(id),
    invoice_id uuid NOT NULL REFERENCES invoices(id),
    amount numeric(12, 2) NOT NULL,
    payment_date date NOT NULL,
    method varchar(30) NOT NULL,
    reference varchar(100),
    notes text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_appointments_garage_start ON appointments (garage_id, start_date_time);
CREATE INDEX IF NOT EXISTS idx_work_orders_garage_status ON work_orders (garage_id, status);
CREATE INDEX IF NOT EXISTS idx_quotes_garage_status ON quotes (garage_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_garage_status ON invoices (garage_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_garage_invoice ON payments (garage_id, invoice_id);

ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE work_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

--rollback DROP TABLE IF EXISTS payments, invoice_items, invoices, quote_items, quotes, work_order_items, work_orders, appointments;
