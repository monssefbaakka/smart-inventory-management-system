package com.example.smartinventory.model;

import java.util.Arrays;
import java.util.List;

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

    /** The states goods are still expected in, worked out once from the states themselves. */
    private static final List<PurchaseOrderStatus> AWAITING_DELIVERY = Arrays.stream(values())
            .filter(PurchaseOrderStatus::isAwaitingDelivery)
            .toList();

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

    /**
     * Returns the states an order is still waiting on the supplier in, for the queries that ask
     * after outstanding orders by status.
     *
     * <p>Derived from {@link #isAwaitingDelivery()} rather than listed again, so a state added here
     * cannot be waited on in one place and not in another.
     *
     * @return every state in which goods are still expected against an order
     */
    public static List<PurchaseOrderStatus> awaitingDelivery() {
        return AWAITING_DELIVERY;
    }
}
