package com.example.smartinventory.notification;

/** Kind of stock condition that warrants a notification. */
public enum StockEventType {

    /** Product quantity is at or below its reorder threshold but still above zero. */
    LOW_STOCK,

    /** Product quantity has reached zero. */
    OUT_OF_STOCK

}
