package com.example.smartinventory.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * Opt-in channel that emails stock events to a configured recipient list.
 *
 * <p>Activated only when {@code notifications.email.enabled=true}. Delivery failures are
 * logged and swallowed so a mail-server outage never affects the stock movement transaction.
 */
@Component
@ConditionalOnProperty(prefix = "notifications.email", name = "enabled", havingValue = "true")
public class EmailStockEventNotifier implements StockEventNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailStockEventNotifier.class);

    private final MailSender mailSender;
    private final String from;
    private final String[] recipients;

    /**
     * Creates the email channel.
     *
     * @param mailSender the sender used to deliver messages
     * @param from       address that appears as the message sender
     * @param recipients comma-separated list of destination addresses
     */
    public EmailStockEventNotifier(MailSender mailSender,
            @Value("${notifications.email.from:}") String from,
            @Value("${notifications.email.to:}") String[] recipients) {
        this.mailSender = mailSender;
        this.from = from;
        this.recipients = recipients;
    }

    @Override
    public void send(StockEventNotification notification) {
        if (from == null || from.isBlank() || recipients == null || recipients.length == 0) {
            LOGGER.warn("Email notifications enabled but notifications.email.from/to not configured; skipping");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients);
            message.setSubject(subject(notification));
            message.setText(body(notification));
            mailSender.send(message);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to email stock event {} for product {}",
                    notification.eventType(), notification.productId(), ex);
        }
    }

    private String subject(StockEventNotification notification) {
        String site = notification.warehouseCode() == null ? "" : " at " + notification.warehouseCode();
        return "[Inventory] " + notification.eventType() + ": " + notification.name()
                + " (" + notification.sku() + ")" + site;
    }

    private String body(StockEventNotification notification) {
        String location = notification.warehouseCode() == null
                ? ""
                : "Warehouse: " + notification.warehouseCode() + " (id=" + notification.warehouseId() + ")"
                        + System.lineSeparator();
        return "Stock event: " + notification.eventType() + System.lineSeparator()
                + "Product: " + notification.name() + " (id=" + notification.productId()
                + ", sku=" + notification.sku() + ")" + System.lineSeparator()
                + location
                + "Current quantity: " + notification.quantity() + System.lineSeparator()
                + "Reorder threshold: " + notification.reorderThreshold() + System.lineSeparator()
                + "Detected at: " + notification.occurredAt();
    }

}
