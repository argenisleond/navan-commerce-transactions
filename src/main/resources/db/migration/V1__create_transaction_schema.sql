CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    total DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_transactions_external_id UNIQUE (external_id),
    CONSTRAINT ck_transactions_type
        CHECK (type IN ('FLIGHT', 'HOTEL', 'RAIL', 'NAVAN_FEE'))
);

CREATE INDEX idx_transactions_created_at_id ON transactions (created_at, id);

CREATE TABLE transaction_tax_lines (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    rate DECIMAL(9, 6),
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_transaction_tax_lines_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT uk_transaction_tax_lines_order UNIQUE (transaction_id, line_order)
);

CREATE INDEX idx_transaction_tax_lines_transaction
    ON transaction_tax_lines (transaction_id);

CREATE TABLE transaction_fee_lines (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_transaction_fee_lines_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT uk_transaction_fee_lines_order UNIQUE (transaction_id, line_order)
);

CREATE INDEX idx_transaction_fee_lines_transaction
    ON transaction_fee_lines (transaction_id);

CREATE TABLE flight_details (
    transaction_id UUID PRIMARY KEY,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    fare DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_flight_details_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE TABLE flight_coupons (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    flight_number VARCHAR(64) NOT NULL,
    carrier VARCHAR(64),
    from_location VARCHAR(255) NOT NULL,
    to_location VARCHAR(255) NOT NULL,
    departure TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    arrival TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_flight_coupons_details
        FOREIGN KEY (transaction_id) REFERENCES flight_details (transaction_id) ON DELETE CASCADE,
    CONSTRAINT uk_flight_coupons_order UNIQUE (transaction_id, line_order)
);

CREATE INDEX idx_flight_coupons_transaction ON flight_coupons (transaction_id);

CREATE TABLE hotel_details (
    transaction_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address_street VARCHAR(255) NOT NULL,
    address_city VARCHAR(255) NOT NULL,
    address_country VARCHAR(255) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    nights INTEGER NOT NULL,
    room_rate_per_night DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_hotel_details_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE TABLE hotel_extras (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_hotel_extras_details
        FOREIGN KEY (transaction_id) REFERENCES hotel_details (transaction_id) ON DELETE CASCADE,
    CONSTRAINT uk_hotel_extras_order UNIQUE (transaction_id, line_order)
);

CREATE INDEX idx_hotel_extras_transaction ON hotel_extras (transaction_id);

CREATE TABLE rail_legs (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    line_order INTEGER NOT NULL,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    departure TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    arrival TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    train_number VARCHAR(64),
    travel_class VARCHAR(64),
    CONSTRAINT fk_rail_legs_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT uk_rail_legs_order UNIQUE (transaction_id, line_order)
);

CREATE INDEX idx_rail_legs_transaction ON rail_legs (transaction_id);

CREATE TABLE navan_fee_details (
    transaction_id UUID PRIMARY KEY,
    fee_kind VARCHAR(32) NOT NULL,
    description VARCHAR(1000),
    related_external_id VARCHAR(255),
    CONSTRAINT fk_navan_fee_details_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT ck_navan_fee_details_kind
        CHECK (fee_kind IN ('TRIP_FEE', 'AGENT_CALL_FEE'))
);
