package com.navan.commerce.transactions.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CreateInvoiceIntegrationTest extends ApiIntegrationTest {

    @Test
    void createsATripInvoiceWithTransactionSnapshotsAndSeparateRollups() throws Exception {
        var transactions = createTripTransactions();

        var invoice = createInvoice(transactions.ids());

        assertThatCode(() -> UUID.fromString(invoice.path("id").textValue()))
                .doesNotThrowAnyException();
        assertThat(invoice.path("currency").textValue()).isEqualTo("EUR");
        assertThat(invoice.path("total").decimalValue()).isEqualByComparingTo("685.70");
        assertThat(lineTransactionIds(invoice)).containsExactlyElementsOf(transactions.ids());
        assertSnapshot(findLine(invoice, transactions.flight().id()), transactions.flight());
        assertSnapshot(findLine(invoice, transactions.hotel().id()), transactions.hotel());
        assertSnapshot(findLine(invoice, transactions.rail().id()), transactions.rail());
        assertSnapshot(findLine(invoice, transactions.tripFee().id()), transactions.tripFee());
        assertSnapshot(findLine(invoice, transactions.agentCallFee().id()),
                transactions.agentCallFee());

        assertThat(invoice.path("tax_rollup")).hasSize(2);
        assertThat(invoice.path("tax_rollup").findValuesAsText("name"))
                .containsExactly("Airport tax", "VAT");
        assertThat(invoice.at("/tax_rollup/0/amount").decimalValue())
                .isEqualByComparingTo("15.00");
        assertThat(invoice.at("/tax_rollup/1/rate").decimalValue())
                .isEqualByComparingTo("0.070000");
        assertThat(invoice.at("/tax_rollup/1/amount").decimalValue())
                .isEqualByComparingTo("21.70");

        assertThat(invoice.path("fee_rollup")).hasSize(2);
        assertThat(invoice.path("fee_rollup").findValuesAsText("name"))
                .containsExactly("YQ", "Booking fee");
        assertThat(invoice.at("/fee_rollup/0/amount").decimalValue())
                .isEqualByComparingTo("25.00");
        assertThat(invoice.at("/fee_rollup/1/amount").decimalValue())
                .isEqualByComparingTo("4.00");
    }

    @Test
    void acceptsTheSingleTransactionRequestShape() throws Exception {
        var flight = createStoredTransaction("flight.json");

        var invoice = createSingleTransactionInvoice(flight.id());

        assertThat(invoice.path("currency").textValue()).isEqualTo("EUR");
        assertThat(invoice.path("total").decimalValue()).isEqualByComparingTo("220.00");
        assertThat(lineTransactionIds(invoice)).containsExactly(flight.id());
    }

    @Test
    void returnsNotFoundWhenAnyTransactionDoesNotExist() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var missingId = UUID.randomUUID().toString();

        var response = postInvoice(
                multipleTransactionBody(List.of(flight.id(), missingId)), HttpStatus.NOT_FOUND);

        assertThat(response.path("message").textValue())
                .isEqualTo("Transaction not found for id " + missingId);
        assertInvoiceListIsEmpty();
    }

    @Test
    void rejectsRepeatedTransactionIdsWithoutCreatingAnInvoice() throws Exception {
        var flight = createStoredTransaction("flight.json");

        var response = postInvoice(
                multipleTransactionBody(List.of(flight.id(), flight.id())), HttpStatus.CONFLICT);

        assertThat(response.path("message").textValue())
                .isEqualTo("Transaction ids must be unique within an invoice");
        assertInvoiceListIsEmpty();
    }

    @Test
    void rejectsMixedCurrenciesWithoutCreatingAnInvoice() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var hotelBody = (ObjectNode) readFixture(TRANSACTION_FIXTURE_PATH + "hotel.json");
        hotelBody.put("currency", "USD");
        hotelBody.put("external_id", "hotel-usd");
        var hotel = createStoredTransaction(hotelBody);

        var response = postInvoice(
                multipleTransactionBody(List.of(flight.id(), hotel.id())), HttpStatus.CONFLICT);

        assertThat(response.path("message").textValue())
                .isEqualTo("All transactions on an invoice must use the same currency");
        assertInvoiceListIsEmpty();
    }

    @Test
    void rejectsAnEmptyTransactionList() throws Exception {
        postInvoice(multipleTransactionBody(List.of()), HttpStatus.BAD_REQUEST);
        assertInvoiceListIsEmpty();
    }

    @Test
    void groupsTaxesByNameAndRateAndFeesByName() throws Exception {
        var firstFlight = createStoredTransaction("flight.json");
        var secondFlightBody = (ObjectNode) readFixture(TRANSACTION_FIXTURE_PATH + "flight.json");
        secondFlightBody.put("external_id", "second-flight");
        var secondFlight = createStoredTransaction(secondFlightBody);
        var firstHotel = createStoredTransaction("hotel.json");
        var secondHotelBody = (ObjectNode) readFixture(TRANSACTION_FIXTURE_PATH + "hotel.json");
        secondHotelBody.put("external_id", "second-hotel");
        ((ObjectNode) secondHotelBody.at("/tax_lines/0")).put("rate", 0.20);
        var secondHotel = createStoredTransaction(secondHotelBody);

        var invoice = createInvoice(List.of(
                firstFlight.id(), secondFlight.id(), firstHotel.id(), secondHotel.id()));

        assertThat(invoice.path("tax_rollup")).hasSize(3);
        assertThat(invoice.path("tax_rollup").findValuesAsText("name"))
                .containsExactly("Airport tax", "VAT", "VAT");
        assertThat(invoice.at("/tax_rollup/0/amount").decimalValue())
                .isEqualByComparingTo("30.00");
        assertThat(invoice.at("/tax_rollup/1/rate").decimalValue())
                .isEqualByComparingTo("0.070000");
        assertThat(invoice.at("/tax_rollup/2/rate").decimalValue())
                .isEqualByComparingTo("0.200000");
        assertThat(invoice.path("fee_rollup")).hasSize(1);
        assertThat(invoice.at("/fee_rollup/0/name").textValue()).isEqualTo("YQ");
        assertThat(invoice.at("/fee_rollup/0/amount").decimalValue())
                .isEqualByComparingTo("50.00");
    }

    private void assertSnapshot(com.fasterxml.jackson.databind.JsonNode line,
                                CreatedTransaction transaction) {
        var expected = transaction.requestBody();
        assertThat(line.path("transaction_id").textValue()).isEqualTo(transaction.id());
        assertThat(line.path("type")).isEqualTo(expected.path("type"));
        assertThat(line.path("external_id")).isEqualTo(expected.path("external_id"));
        assertThat(line.path("currency")).isEqualTo(expected.path("currency"));
        assertThat(line.path("occurred_at")).isEqualTo(expected.path("occurred_at"));
        assertThat(line.path("total").decimalValue())
                .isEqualByComparingTo(expected.path("total").decimalValue());
        assertThat(line.path("tax_lines")).isEqualTo(expected.path("tax_lines"));
        assertThat(line.path("fee_lines")).isEqualTo(expected.path("fee_lines"));
        assertThat(line.path("metadata")).isEqualTo(expected.path("metadata"));
    }

    private void assertInvoiceListIsEmpty() throws Exception {
        var invoices = objectMapper.readTree(request()
                .get("/invoices")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().asString());
        assertThat(invoices.path("items")).isEmpty();
    }
}
