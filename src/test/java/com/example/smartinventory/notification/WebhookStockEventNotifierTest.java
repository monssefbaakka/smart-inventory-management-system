package com.example.smartinventory.notification;

import java.time.Instant;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WebhookStockEventNotifierTest {

    private static final String URL = "https://hooks.example.com/stock";

    private StockEventNotification lowStock() {
        return new StockEventNotification(
                1L, "SKU-1", "Widget", 3, 10, StockEventType.LOW_STOCK, Instant.now());
    }

    @Test
    void postsJsonToConfiguredUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"eventType\":\"LOW_STOCK\"")))
                .andRespond(withSuccess());

        WebhookStockEventNotifier notifier = new WebhookStockEventNotifier(builder, URL);
        notifier.send(lowStock());

        server.verify();
    }

    @Test
    void skipsWhenUrlBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        WebhookStockEventNotifier notifier = new WebhookStockEventNotifier(builder, "");
        notifier.send(lowStock());

        server.verify();
    }

    @Test
    void swallowsDeliveryFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withServerError());

        WebhookStockEventNotifier notifier = new WebhookStockEventNotifier(builder, URL);
        notifier.send(lowStock());

        server.verify();
    }

}
