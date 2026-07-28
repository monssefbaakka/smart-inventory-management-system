package com.example.smartinventory.model;

/** Direction of a {@link StockMovement}. */
public enum MovementType {

    IN,
    OUT,
    ADJUSTMENT,

    /** Leg recording stock leaving the source warehouse of a {@link StockTransfer}. */
    TRANSFER_OUT,

    /** Leg recording stock arriving at the destination warehouse of a {@link StockTransfer}. */
    TRANSFER_IN;

    /**
     * Reports whether this type is one leg of a warehouse-to-warehouse transfer, which moves stock
     * between locations without changing the product's overall quantity.
     *
     * @return {@code true} for {@code TRANSFER_OUT} and {@code TRANSFER_IN}
     */
    public boolean isTransferLeg() {
        return this == TRANSFER_OUT || this == TRANSFER_IN;
    }

}
