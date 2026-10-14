package com.navan.commerce.transactions.mapper;

import com.navan.commerce.transactions.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionResponseMapperTest {

    private final TransactionResponseMapper mapper = new TransactionResponseMapper();

    @Test
    void mapsEveryTypedMetadataShape() {
        var responses = List.of(
                mapper.toResponse(MapperFixtures.flight("flight", "EUR", "220.00")),
                mapper.toResponse(MapperFixtures.hotel("hotel", "EUR", "331.70")),
                mapper.toResponse(MapperFixtures.rail("rail", "EUR", "94.00")),
                mapper.toResponse(MapperFixtures.navanFee("fee", "EUR", "25.00")));

        assertThat(responses.get(0).metadata())
                .isInstanceOf(TransactionResponse.FlightMetadata.class);
        assertThat(responses.get(1).metadata())
                .isInstanceOf(TransactionResponse.HotelMetadata.class);
        assertThat(responses.get(2).metadata())
                .isInstanceOf(TransactionResponse.RailMetadata.class);
        assertThat(responses.get(3).metadata())
                .isInstanceOf(TransactionResponse.NavanFeeMetadata.class);
        assertThat(responses.getFirst().taxLines()).singleElement()
                .satisfies(line -> assertThat(line.name()).isEqualTo("Airport tax"));
        assertThat(responses.getFirst().feeLines()).singleElement()
                .satisfies(line -> assertThat(line.name()).isEqualTo("YQ"));
    }

    @Test
    void mapsPaginationMetadataWithoutChangingItemOrder() {
        var first = MapperFixtures.flight("first", "EUR", "220.00");
        var second = MapperFixtures.hotel("second", "EUR", "331.70");

        var page = mapper.toPage(List.of(first, second), 2, 4, true);

        assertThat(page.items()).extracting(TransactionResponse::externalId)
                .containsExactly("first", "second");
        assertThat(page.limit()).isEqualTo(2);
        assertThat(page.offset()).isEqualTo(4);
        assertThat(page.hasMore()).isTrue();
    }
}
