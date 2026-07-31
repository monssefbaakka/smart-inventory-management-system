package com.example.smartinventory.dto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.smartinventory.exception.InvalidQueryParameterException;

class PageRequestsTest {

    private static final List<String> SORTABLE = List.of("id", "createdAt", "quantity");

    @Test
    void buildsThePageRequestAsked() {
        PageRequest request = PageRequests.of(2, 5, "quantity,desc", SORTABLE);

        assertThat(request).isEqualTo(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "quantity")));
    }

    @Test
    void sortsAscendingWhenNoDirectionIsGiven() {
        assertThat(PageRequests.of(0, 20, "createdAt", SORTABLE).getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Test
    void sortsAscendingWhenTheDirectionIsBlank() {
        assertThat(PageRequests.of(0, 20, "createdAt, ", SORTABLE).getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(PageRequests.of(0, 20, " id , desc ", SORTABLE).getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> PageRequests.of(-1, 20, "id", SORTABLE))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("page must not be negative");
    }

    @Test
    void rejectsAnEmptyPage() {
        assertThatThrownBy(() -> PageRequests.of(0, 0, "id", SORTABLE))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void rejectsAPageBeyondTheCap() {
        assertThatThrownBy(() -> PageRequests.of(0, PageRequests.MAX_PAGE_SIZE + 1, "id", SORTABLE))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void acceptsExactlyTheCap() {
        assertThat(PageRequests.of(0, PageRequests.MAX_PAGE_SIZE, "id", SORTABLE).getPageSize())
                .isEqualTo(PageRequests.MAX_PAGE_SIZE);
    }

    @Test
    void rejectsAFieldOutsideTheAllowlistAndNamesTheOnesAllowed() {
        assertThatThrownBy(() -> PageRequests.of(0, 20, "tenantId", SORTABLE))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("Cannot sort by 'tenantId'")
                .hasMessageContaining("id, createdAt, quantity");
    }

    @Test
    void rejectsAnUnknownDirection() {
        assertThatThrownBy(() -> PageRequests.of(0, 20, "id,sideways", SORTABLE))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("Unknown sort direction 'sideways'");
    }

}
