package com.example.smartinventory.dto;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.smartinventory.exception.InvalidQueryParameterException;

/**
 * Turns the {@code page}, {@code size} and {@code sort} query parameters of a listing endpoint into
 * a {@link PageRequest}.
 *
 * <p>Every listing shares one cap and one set of rejections, so a caller cannot pull an unbounded
 * response out of any of them, and an ordering the query cannot serve is answered as a client error
 * rather than reaching the database.
 */
public final class PageRequests {

    /** Page size used when the caller names none. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Largest page a caller may ask for, so no single call can pull a whole table. */
    public static final int MAX_PAGE_SIZE = 100;

    /** Ordering used by listings that have no inherent order. */
    public static final String ID_ASCENDING = "id,asc";

    /** Ordering used by the history listings, which read newest first. */
    public static final String NEWEST_FIRST = "createdAt,desc";

    private PageRequests() {
    }

    /**
     * Builds the page request for a listing.
     *
     * @param page           zero-based index of the page asked for
     * @param size           number of results asked for, at most {@link #MAX_PAGE_SIZE}
     * @param sort           {@code field} or {@code field,asc|desc}
     * @param sortableFields the fields this listing can be ordered by
     * @return the page request to run the listing query with
     * @throws InvalidQueryParameterException if the page, size or ordering cannot be served
     */
    public static PageRequest of(int page, int size, String sort, List<String> sortableFields) {
        if (page < 0) {
            throw new InvalidQueryParameterException("page must not be negative, but was " + page);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidQueryParameterException(
                    "size must be between 1 and " + MAX_PAGE_SIZE + ", but was " + size);
        }
        return PageRequest.of(page, size, parseSort(sort, sortableFields));
    }

    private static Sort parseSort(String sort, List<String> sortableFields) {
        String[] parts = sort.split(",", 2);
        String field = parts[0].strip();
        if (!sortableFields.contains(field)) {
            throw new InvalidQueryParameterException(
                    "Cannot sort by '" + field + "'; sortable fields are " + String.join(", ", sortableFields));
        }
        if (parts.length == 1 || parts[1].isBlank()) {
            return Sort.by(Sort.Direction.ASC, field);
        }
        String direction = parts[1].strip();
        return Sort.by(Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidQueryParameterException(
                        "Unknown sort direction '" + direction + "'; use asc or desc")), field);
    }

}
