package com.example.smartinventory.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Opt-in channel that POSTs stock events as JSON to a configured webhook URL.
 *
 * <p>Activated only when {@code notifications.webhook.enabled=true}. Delivery failures are
 * logged and swallowed so a webhook outage never affects the stock movement transaction.
 */
@Component
@ConditionalOnProperty(prefix = "notifications.webhook", name = "enabled", havingValue = "true")
public class WebhookStockEventNotifier implements StockEventNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookStockEventNotifier.class);

    private final RestClient restClient;
    private final String url;

    /**
     * Creates the webhook channel.
     *
     * @param restClientBuilder builder used to construct the HTTP client
     * @param url               destination URL that receives the JSON payload
     */
    public WebhookStockEventNotifier(RestClient.Builder restClientBuilder,
            @Value("${notifications.webhook.url:}") String url) {
        this.restClient = restClientBuilder.build();
        this.url = url;
    }

    @Override
    public void send(StockEventNotification notification) {
        if (url == null || url.isBlank()) {
            LOGGER.warn("Webhook notifications enabled but no notifications.webhook.url configured; skipping");
            return;
        }
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to deliver stock event {} for product {} to webhook {}",
                    notification.eventType(), notification.productId(), url, ex);
        }
    }

}
