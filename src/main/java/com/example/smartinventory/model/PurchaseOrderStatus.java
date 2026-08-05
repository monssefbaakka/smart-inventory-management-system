package com.example.smartinventory.model;

/** Lifecycle state of a {@link PurchaseOrder}. */
public enum PurchaseOrderStatus {

    /** Editable draft that has not yet been sent to the supplier. */
    DRAFT,

    /** Order committed and sent to the supplier, awaiting delivery. */
    PLACED,

    /** Some of the goods have arrived and been applied to stock; the rest is still outstanding. */
    PARTIALLY_RECEIVED,

    /** Goods received in full; every line item has been applied to stock. */
    RECEIVED,

    /** Order abandoned; whatever had already been received stays, the outstanding quantity does not. */
    CANCELLED
}
