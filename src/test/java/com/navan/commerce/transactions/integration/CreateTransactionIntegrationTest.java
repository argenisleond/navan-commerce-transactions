package com.navan.commerce.transactions.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CreateTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FIXTURE_PATH = "fixtures/transactions/";

    @Test
    void createsFlightWithCouponsTaxesAndFees() throws Exception {
        JsonNode response = createTransaction("flight.json");

        assertThat(response.at("/metadata/coupons")).hasSize(1);
        assertThat(response.at("/metadata/coupons/0/flight_number").textValue())
                .isEqualTo("BA982");
    }

    @Test
    void createsHotelWithAddressAndExtras() throws Exception {
        JsonNode response = createTransaction("hotel.json");

        assertThat(response.at("/metadata/address/city").textValue()).isEqualTo("London");
        assertThat(response.at("/metadata/extras")).hasSize(1);
    }

    @Test
    void createsRailJourneyWithMultipleLegs() throws Exception {
        JsonNode response = createTransaction("rail.json");

        assertThat(response.at("/metadata/legs")).hasSize(2);
        assertThat(response.at("/metadata/legs/1/destination").textValue())
                .isEqualTo("Paris Nord");
    }

    @Test
    void createsTripFee() throws Exception {
        JsonNode response = createTransaction("trip-fee.json");

        assertThat(response.at("/metadata/fee_kind").textValue()).isEqualTo("trip_fee");
    }

    @Test
    void createsAgentCallFee() throws Exception {
        JsonNode response = createTransaction("agent-call-fee.json");

        assertThat(response.at("/metadata/fee_kind").textValue()).isEqualTo("agent_call_fee");
    }

    private JsonNode createTransaction(String fixtureName) throws Exception {
        JsonNode requestBody = readFixture(FIXTURE_PATH + fixtureName);

        var response = request()
                .body(requestBody.toString())
                .when()
                .post("/transactions")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .extract().response();

        JsonNode responseBody = objectMapper.readTree(response.asString());

        assertThat(responseBody.path("id").textValue()).isNotBlank();
        assertThatCode(() -> UUID.fromString(responseBody.path("id").textValue()))
                .doesNotThrowAnyException();
        JSONAssert.assertEquals(requestBody.toString(), responseBody.toString(),
                JSONCompareMode.STRICT_ORDER);

        return responseBody;
    }
}
