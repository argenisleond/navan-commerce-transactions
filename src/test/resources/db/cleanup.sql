SET REFERENTIAL_INTEGRITY FALSE;

TRUNCATE TABLE flight_coupons;
TRUNCATE TABLE flight_details;
TRUNCATE TABLE hotel_extras;
TRUNCATE TABLE hotel_details;
TRUNCATE TABLE rail_legs;
TRUNCATE TABLE navan_fee_details;
TRUNCATE TABLE transaction_fee_lines;
TRUNCATE TABLE transaction_tax_lines;
TRUNCATE TABLE transactions;

SET REFERENTIAL_INTEGRITY TRUE;
