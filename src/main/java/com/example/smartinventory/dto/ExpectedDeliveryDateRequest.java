package com.example.smartinventory.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Request payload for re-promising an order the supplier has given a new date for. */
@Schema(description = "A revised date the goods on an order are now expected to arrive")
public record ExpectedDeliveryDateRequest(

        @NotNull
        @Schema(description = "The day the goods are now expected. May be earlier as well as later than the "
                + "date the order carries; the date it was originally promised for is kept either way.",
                example = "2026-09-22")
        LocalDate expectedDeliveryDate) {
}
