package com.example.smartinventory.model;

/** Lifecycle status of a {@link StockCount}. */
public enum StockCountStatus {

    /** Being counted; lines may still be added, and nothing has touched stock yet. */
    DRAFT,

    /** Counted and applied; every line has adjusted the warehouse's stock. */
    COMPLETED,

    /** Abandoned before completion; stock is untouched. */
    CANCELLED
}
