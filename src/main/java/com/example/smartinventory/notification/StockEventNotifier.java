package com.example.smartinventory.notification;

/** A channel capable of delivering a {@link StockEventNotification} to some destination. */
public interface StockEventNotifier {

    /**
     * Delivers the given notification through this channel.
     *
     * @param notification the stock event to deliver
     */
    void send(StockEventNotification notification);

}
