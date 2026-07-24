package com.example.smartinventory.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.notification.StockEventNotification;
import com.example.smartinventory.notification.StockEventNotifier;
import com.example.smartinventory.notification.StockEventType;

@ExtendWith(MockitoExtension.class)
class StockEventNotificationServiceTest {

    @Mock
    private StockEventNotifier notifierA;

    @Mock
    private StockEventNotifier notifierB;

    private StockEventNotificationService service(StockEventNotifier... notifiers) {
        return new StockEventNotificationService(List.of(notifiers));
    }

    @Test
    void dispatchesLowStockToAllChannels() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget")
                .quantity(5).reorderThreshold(10).build();

        service(notifierA, notifierB).evaluate(product);

        ArgumentCaptor<StockEventNotification> captor = ArgumentCaptor.forClass(StockEventNotification.class);
        verify(notifierA).send(captor.capture());
        verify(notifierB).send(any(StockEventNotification.class));
        StockEventNotification sent = captor.getValue();
        assertThat(sent.eventType()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(sent.productId()).isEqualTo(1L);
        assertThat(sent.sku()).isEqualTo("SKU-1");
        assertThat(sent.occurredAt()).isNotNull();
    }

    @Test
    void dispatchesOutOfStockWhenQuantityZero() {
        Product product = Product.builder().id(2L).sku("SKU-2").name("Gadget")
                .quantity(0).reorderThreshold(10).build();

        service(notifierA).evaluate(product);

        ArgumentCaptor<StockEventNotification> captor = ArgumentCaptor.forClass(StockEventNotification.class);
        verify(notifierA).send(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(StockEventType.OUT_OF_STOCK);
    }

    @Test
    void skipsNotificationWhenAboveThreshold() {
        Product product = Product.builder().id(3L).quantity(50).reorderThreshold(10).build();

        service(notifierA).evaluate(product);

        verify(notifierA, never()).send(any());
    }

    @Test
    void skipsNotificationWhenQuantityOrThresholdMissing() {
        Product noQuantity = Product.builder().id(4L).quantity(null).reorderThreshold(10).build();
        Product noThreshold = Product.builder().id(5L).quantity(5).reorderThreshold(null).build();

        StockEventNotificationService service = service(notifierA);
        service.evaluate(noQuantity);
        service.evaluate(noThreshold);

        verify(notifierA, never()).send(any());
    }

    @Test
    void continuesDispatchWhenAChannelThrows() {
        Product product = Product.builder().id(6L).sku("SKU-6").name("Thing")
                .quantity(1).reorderThreshold(10).build();
        doThrow(new RuntimeException("boom")).when(notifierA).send(any());

        service(notifierA, notifierB).evaluate(product);

        verify(notifierB).send(any(StockEventNotification.class));
    }

}
