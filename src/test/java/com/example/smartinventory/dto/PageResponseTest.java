package com.example.smartinventory.dto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void carriesTheMappedContentAndThePagingMetadata() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(1, 2), 6);

        PageResponse<String> response = PageResponse.from(page, String::valueOf);

        assertThat(response.content()).containsExactly("1", "2");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void marksTheOnlyPageAsBothFirstAndLast() {
        PageResponse<Integer> response =
                PageResponse.from(new PageImpl<>(List.of(1), PageRequest.of(0, 20), 1), value -> value);

        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void carriesAnEmptyPage() {
        PageResponse<Integer> response = PageResponse.from(Page.<Integer>empty(PageRequest.of(0, 20)), value -> value);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

}
