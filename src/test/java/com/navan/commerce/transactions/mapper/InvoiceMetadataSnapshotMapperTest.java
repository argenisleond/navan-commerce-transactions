package com.navan.commerce.transactions.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.transactions.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceMetadataSnapshotMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InvoiceMetadataSnapshotMapper mapper = new InvoiceMetadataSnapshotMapper();

    @Test
    void definesThePersistedMetadataShapeForEveryTransactionType() throws Exception {
        var flight = metadata(MapperFixtures.flight("flight", "EUR", "220.00"));
        var hotel = metadata(MapperFixtures.hotel("hotel", "EUR", "331.70"));
        var rail = metadata(MapperFixtures.rail("rail", "EUR", "90.00"));
        var navanFee = metadata(MapperFixtures.navanFee("fee", "EUR", "14.00"));

        assertThat(fieldNames(flight))
                .containsExactlyInAnyOrder("origin", "destination", "fare", "coupons");
        assertThat(fieldNames(flight.path("coupons").get(0)))
                .containsExactlyInAnyOrder(
                        "flight_number", "carrier", "from", "to", "departure", "arrival");
        assertThat(flight.path("coupons").get(0).path("flight_number").textValue())
                .isEqualTo("BA982");
        assertThat(flight.path("coupons").get(0).path("departure").textValue())
                .isEqualTo("2026-03-11T07:00:00Z");
        assertThat(fieldNames(hotel))
                .containsExactlyInAnyOrder(
                        "name", "address", "check_in", "check_out", "nights",
                        "room_rate_per_night", "extras");
        assertThat(fieldNames(hotel.path("address")))
                .containsExactlyInAnyOrder("street", "city", "country");
        assertThat(hotel.path("address").path("country").textValue()).isEqualTo("GB");
        assertThat(hotel.path("room_rate_per_night").decimalValue())
                .isEqualByComparingTo("140.00");
        assertThat(fieldNames(rail)).containsExactly("legs");
        assertThat(fieldNames(rail.path("legs").get(0)))
                .containsExactlyInAnyOrder(
                        "origin", "destination", "departure", "arrival", "train_number", "class");
        assertThat(rail.path("legs").get(0).path("train_number").textValue())
                .isEqualTo("EST9108");
        assertThat(rail.path("legs").get(0).path("class").textValue())
                .isEqualTo("standard");
        assertThat(fieldNames(navanFee))
                .containsExactlyInAnyOrder("fee_kind", "description", "related_external_id");
        assertThat(navanFee.path("fee_kind").textValue()).isEqualTo("trip_fee");
        assertThat(navanFee.path("related_external_id").textValue())
                .isEqualTo("flight-reference");
    }

    private JsonNode metadata(Transaction transaction) throws Exception {
        return objectMapper.readTree(mapper.toJson(transaction));
    }

    private List<String> fieldNames(JsonNode node) {
        var fields = new ArrayList<String>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }
}
