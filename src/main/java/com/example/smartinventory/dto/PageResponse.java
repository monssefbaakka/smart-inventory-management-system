package com.example.smartinventory.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of results together with the metadata a caller needs to walk the rest of them.
 *
 * <p>Pages are returned through this envelope rather than through Spring Data's {@code Page}, whose
 * serialised form is not part of its API contract and changes between versions.
 *
 * @param <T> type of the results carried on the page
 */
@Schema(description = "One page of results")
public record PageResponse<T>(

        @Schema(description = "The results on this page")
        List<T> content,

        @Schema(description = "Zero-based index of this page", example = "0")
        int page,

        @Schema(description = "Maximum number of results a page may carry", example = "20")
        int size,

        @Schema(description = "Total number of results across every page", example = "137")
        long totalElements,

        @Schema(description = "Total number of pages available", example = "7")
        int totalPages,

        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last) {

    /**
     * Wraps a page of entities, converting each one into its response form.
     *
     * @param page   the page as returned by the repository
     * @param mapper converts a single entity into its response payload
     * @param <E>    entity type carried by the page
     * @param <T>    response type carried by the envelope
     * @return the page envelope
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

}
