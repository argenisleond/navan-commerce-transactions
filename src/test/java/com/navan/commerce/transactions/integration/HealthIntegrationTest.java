package com.navan.commerce.transactions.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class HealthIntegrationTest extends ApiIntegrationTest {

    @Test
    void reportsThatTheApplicationIsUp() throws Exception {
        var response = request()
                .get("/health")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().asString();

        assertThat(objectMapper.readTree(response).path("status").textValue()).isEqualTo("UP");
    }
}
