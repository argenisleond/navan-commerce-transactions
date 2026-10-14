CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,
    total DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoices_created_at_id ON invoices (created_at, id);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    source_transaction_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    total DECIMAL(19, 2) NOT NULL,
    metadata_json CLOB NOT NULL,
    CONSTRAINT fk_invoice_lines_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_lines_transaction
        FOREIGN KEY (source_transaction_id) REFERENCES transactions (id),
    CONSTRAINT uk_invoice_lines_order UNIQUE (invoice_id, line_order)
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
CREATE INDEX idx_invoice_lines_source_transaction ON invoice_lines (source_transaction_id);

CREATE TABLE invoice_tax_lines (
    id UUID PRIMARY KEY,
    invoice_line_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    rate DECIMAL(9, 6),
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_invoice_tax_lines_line
        FOREIGN KEY (invoice_line_id) REFERENCES invoice_lines (id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_tax_lines_order UNIQUE (invoice_line_id, line_order)
);

CREATE INDEX idx_invoice_tax_lines_line ON invoice_tax_lines (invoice_line_id);

CREATE TABLE invoice_fee_lines (
    id UUID PRIMARY KEY,
    invoice_line_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_invoice_fee_lines_line
        FOREIGN KEY (invoice_line_id) REFERENCES invoice_lines (id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_fee_lines_order UNIQUE (invoice_line_id, line_order)
);

CREATE INDEX idx_invoice_fee_lines_line ON invoice_fee_lines (invoice_line_id);

CREATE TABLE invoice_tax_rollups (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    rate DECIMAL(9, 6),
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_invoice_tax_rollups_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_tax_rollups_order UNIQUE (invoice_id, line_order)
);

CREATE INDEX idx_invoice_tax_rollups_invoice ON invoice_tax_rollups (invoice_id);

CREATE TABLE invoice_fee_rollups (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_invoice_fee_rollups_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_fee_rollups_order UNIQUE (invoice_id, line_order)
);

CREATE INDEX idx_invoice_fee_rollups_invoice ON invoice_fee_rollups (invoice_id);
