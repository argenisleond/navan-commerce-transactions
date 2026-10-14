# Navan Commerce Transactions

A Java 21 and Spring Boot API for storing travel transactions and producing
frozen invoice snapshots.

## Requirements

- JDK 21+
- `jq` for the curl walkthrough below

The Maven Wrapper is included, so Maven does not need to be installed.

## Run

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Data is stored in the file-backed H2
database under `./data`; Flyway creates and migrates the schema automatically.

## Test

```bash
./mvnw test
```

The integration tests start the real HTTP server on a random port, use a
temporary file-backed H2 database, and clean the database before every test.

## API

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/transactions` | Create a flight, hotel, rail, or Navan fee transaction. |
| `GET` | `/transactions/{id}` | Read one transaction. |
| `GET` | `/transactions?limit=50&offset=0` | Read a bounded page; maximum limit is 100. |
| `PATCH` | `/transactions/{id}` | Update supplied mutable fields. |
| `DELETE` | `/transactions/{id}` | Delete an uninvoiced transaction. |
| `POST` | `/invoices` | Create a snapshot from `transaction_id` or `transaction_ids`. |
| `GET` | `/invoices/{id}` | Read one invoice snapshot. |
| `GET` | `/invoices?limit=50&offset=0` | Read a bounded page; maximum limit is 100. |
| `PATCH` | `/invoices/{id}` | Replace all transaction IDs and rebuild the snapshot. |
| `DELETE` | `/invoices/{id}` | Delete the invoice while retaining its transactions. |

Duplicate `external_id` values return `409 Conflict` with the existing
transaction ID. Missing resources return `404`. Transactions referenced by an
invoice cannot be patched or deleted and return `409`. Invoice requests with
mixed currencies or repeated transaction IDs also return `409`.

## Database and volume

Flyway creates a unique index on `transactions.external_id`, primary-key indexes
for transaction and invoice lookup, `(created_at, id)` indexes for ordered lists,
and an index on `invoice_lines.source_transaction_id` for billed-transaction
checks. Child foreign keys are also indexed where they are queried.

List endpoints use database `LIMIT/OFFSET` through a Spring Data `Slice`, ordered
by `(created_at, id)`. Each query reads at most the requested page plus one row to
calculate `has_more`; it does not load the complete table or execute a count
query. The default limit is 50 and the maximum is 100.

## Curl walkthrough

The repository contains the five assignment payloads in `seed-payloads.json`.
Run the following commands in one shell while the application is running.

```bash
BASE_URL=http://localhost:8080

post_seed() {
  jq -c --arg name "$1" '.[$name]' seed-payloads.json |
    curl --silent --show-error \
      -H 'Content-Type: application/json' \
      --data-binary @- \
      "$BASE_URL/transactions"
}

# Create each required transaction type, including both Navan fee kinds.
FLIGHT_ID=$(post_seed flight | jq -r '.id')
HOTEL_ID=$(post_seed hotel | jq -r '.id')
RAIL_ID=$(post_seed rail | jq -r '.id')
TRIP_FEE_ID=$(post_seed trip_fee | jq -r '.id')
AGENT_CALL_FEE_ID=$(post_seed agent_call_fee | jq -r '.id')
```

Posting the flight again returns `409` and the same `FLIGHT_ID`; the database
unique constraint ensures that only one transaction exists even for concurrent
requests.

```bash
jq -c '.flight' seed-payloads.json |
  curl --include --silent --show-error \
    -H 'Content-Type: application/json' \
    --data-binary @- \
    "$BASE_URL/transactions"
```

Read the flight and a transaction page:

```bash
curl --silent --show-error "$BASE_URL/transactions/$FLIGHT_ID" | jq
curl --silent --show-error "$BASE_URL/transactions?limit=50&offset=0" | jq
```

Patch the flight before it is invoiced. Transaction PATCH supports
`occurred_at`, `total`, `tax_lines`, `fee_lines`, and type-compatible `metadata`.

```bash
curl --silent --show-error \
  -X PATCH \
  -H 'Content-Type: application/json' \
  -d '{"occurred_at":"2026-03-11T06:45:00Z"}' \
  "$BASE_URL/transactions/$FLIGHT_ID" | jq
```

Create the five-line trip invoice. Its total is `685.70 EUR`, matching
`expected-invoice.json`. A single-line invoice instead accepts
`{"transaction_id":"<id>"}`.

```bash
INVOICE_RESPONSE=$(
  jq -n \
    --arg flight "$FLIGHT_ID" \
    --arg hotel "$HOTEL_ID" \
    --arg rail "$RAIL_ID" \
    --arg tripFee "$TRIP_FEE_ID" \
    --arg agentFee "$AGENT_CALL_FEE_ID" \
    '{transaction_ids: [$flight, $hotel, $rail, $tripFee, $agentFee]}' |
  curl --silent --show-error \
    -H 'Content-Type: application/json' \
    --data-binary @- \
    "$BASE_URL/invoices"
)
INVOICE_ID=$(printf '%s' "$INVOICE_RESPONSE" | jq -r '.id')
printf '%s\n' "$INVOICE_RESPONSE" | jq
```

Read the invoice and an invoice page:

```bash
curl --silent --show-error "$BASE_URL/invoices/$INVOICE_ID" | jq
curl --silent --show-error "$BASE_URL/invoices?limit=50&offset=0" | jq
```

Because the flight is now invoiced, both mutation attempts return `409` and
leave the transaction and snapshot unchanged:

```bash
curl --include --silent --show-error \
  -X PATCH \
  -H 'Content-Type: application/json' \
  -d '{"total":221.00}' \
  "$BASE_URL/transactions/$FLIGHT_ID"

curl --include --silent --show-error \
  -X DELETE \
  "$BASE_URL/transactions/$FLIGHT_ID"
```

Replace the invoice snapshot with the hotel and rail transactions:

```bash
jq -n --arg hotel "$HOTEL_ID" --arg rail "$RAIL_ID" \
  '{transaction_ids: [$hotel, $rail]}' |
  curl --silent --show-error \
    -X PATCH \
    -H 'Content-Type: application/json' \
    --data-binary @- \
    "$BASE_URL/invoices/$INVOICE_ID" | jq
```

Delete the invoice, verify that its source transaction remains, and then delete
the now-uninvoiced flight:

```bash
curl --include --silent --show-error \
  -X DELETE \
  "$BASE_URL/invoices/$INVOICE_ID"

curl --silent --show-error "$BASE_URL/transactions/$FLIGHT_ID" | jq

curl --include --silent --show-error \
  -X DELETE \
  "$BASE_URL/transactions/$FLIGHT_ID"
```

Implementation decisions and database details are documented in
[`ARCHITECTURE.md`](ARCHITECTURE.md).
