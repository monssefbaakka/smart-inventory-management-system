package com.example.smartinventory.notification;

/** Kind of stock condition that warrants a notification. */
public enum StockEventType {

    /** The measured quantity is at or below the threshold it was measured against, but above zero. */
    LOW_STOCK(1),

    /** The measured quantity has reached zero. */
    OUT_OF_STOCK(2);

    /** How bad the shelf is, on a scale whose only purpose is to order these constants. */
    private final int severity;

    /**
     * Creates a condition of a given severity.
     *
     * @param severity how bad the shelf this condition describes is, higher being worse
     */
    StockEventType(int severity) {
        this.severity = severity;
    }

    /**
     * Says whether this condition is worse than one already announced for the same shelf, so a
     * shortage that deepens is announced while one that merely continues stays quiet.
     *
     * @param announced the condition last announced for that shelf, or {@code null} if none was
     * @return {@code true} when nothing was announced, or when this condition is the worse of the two
     */
    public boolean isWorseThan(StockEventType announced) {
        return announced == null || severity > announced.severity;
    }

}
