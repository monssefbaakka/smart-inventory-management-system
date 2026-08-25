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
    CANCELLED;

    /**
     * Reports whether an order in this state is still waiting on the supplier for goods.
     *
     * <p>A draft is not: nobody has sent it, so nobody owes anything against it. A received order
     * arrived, and a cancelled one is not coming and is not being waited on. A part-delivered order
     * is waiting, for whatever is left on it.
     *
     * @return {@code true} when goods are still expected against an order in this state
     */
    public boolean isAwaitingDelivery() {
        return this == PLACED || this == PARTIALLY_RECEIVED;
    }
}
