package com.example.smartinventory.notification;

import java.time.Instant;

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
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

@ExtendWith(MockitoExtension.class)
class EmailStockEventNotifierTest {

    @Mock
    private MailSender mailSender;

    private StockEventNotification lowStock() {
        return new StockEventNotification(
                1L, "SKU-1", "Widget", 3, 10, StockEventType.LOW_STOCK, Instant.now());
    }

    @Test
    void sendsEmailToConfiguredRecipients() {
        EmailStockEventNotifier notifier = new EmailStockEventNotifier(
                mailSender, "alerts@example.com", new String[] {"a@example.com", "b@example.com"});

        notifier.send(lowStock());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("alerts@example.com");
        assertThat(message.getTo()).containsExactly("a@example.com", "b@example.com");
        assertThat(message.getSubject()).contains("LOW_STOCK").contains("Widget").contains("SKU-1");
        assertThat(message.getText()).contains("Current quantity: 3").contains("Reorder threshold: 10");
    }

    @Test
    void skipsWhenSenderMissing() {
        EmailStockEventNotifier notifier = new EmailStockEventNotifier(
                mailSender, "", new String[] {"a@example.com"});

        notifier.send(lowStock());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenNoRecipients() {
        EmailStockEventNotifier notifier = new EmailStockEventNotifier(
                mailSender, "alerts@example.com", new String[0]);

        notifier.send(lowStock());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void swallowsDeliveryFailure() {
        EmailStockEventNotifier notifier = new EmailStockEventNotifier(
                mailSender, "alerts@example.com", new String[] {"a@example.com"});
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        notifier.send(lowStock());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

}
