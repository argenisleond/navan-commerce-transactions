package com.navan.commerce.transactions.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OffsetPageRequestTest {

    private final Sort sort = Sort.by("createdAt");

    @Test
    void navigatesPagesWhilePreservingTheRequestedOffset() {
        var request = new OffsetPageRequest(25, 10, sort);

        assertThat(request.getOffset()).isEqualTo(25);
        assertThat(request.getPageNumber()).isEqualTo(2);
        assertThat(request.getPageSize()).isEqualTo(10);
        assertThat(request.getSort()).isEqualTo(sort);
        assertThat(request.hasPrevious()).isTrue();
        assertThat(request.next().getOffset()).isEqualTo(35);
        assertThat(request.previousOrFirst().getOffset()).isEqualTo(15);
        assertThat(request.first().getOffset()).isZero();
        assertThat(request.withPage(3).getOffset()).isEqualTo(30);
    }

    @Test
    void rejectsInvalidOffsetsPageSizesAndPageNumbers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OffsetPageRequest(-1, 10, sort));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OffsetPageRequest(0, 0, sort));
        var request = new OffsetPageRequest(0, 10, sort);
        assertThatIllegalArgumentException().isThrownBy(() -> request.withPage(-1));
    }
}
