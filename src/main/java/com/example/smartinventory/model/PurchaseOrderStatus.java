package com.example.smartinventory.model;

/** Lifecycle state of a {@link PurchaseOrder}. */
public enum PurchaseOrderStatus {

    /** Editable draft that has not yet been sent to the supplier. */
    DRAFT,

    /** Order committed and sent to the supplier, awaiting delivery. */
    PLACED,

    /** Goods received; the order's line items have been applied to stock. */
    RECEIVED,

    /** Order abandoned before receipt; no stock effect. */
    CANCELLED
}
