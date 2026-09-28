package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationNotCancellableException;
import kz.company.notification.domain.notification.NotificationNotFoundException;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.event.NotificationCancelled;
import org.junit.jupiter.api.Test;

import static kz.company.notification.application.ApplicationTestContext.CLIENT;
import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelNotificationServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    private NotificationId accept() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        return context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL)).notificationId();
    }

    @Test
    void cancelsPendingNotification() {
        NotificationId id = accept();

        NotificationView view = context.cancelService.cancel(CLIENT, id);
        NotificationView again = context.cancelService.cancel(CLIENT, id);

        assertThat(view.status()).isEqualTo("CANCELLED");
        assertThat(view.deliveries().getFirst().status()).isEqualTo("CANCELLED");
        assertThat(again.status()).isEqualTo("CANCELLED");
        assertThat(context.events.eventsOf(NotificationCancelled.class)).hasSize(1);
    }

    @Test
    void startedNotificationCannotBeCancelled() {
        NotificationId id = accept();
        DeliveryId deliveryId = context.dispatcher.dispatched().getFirst();
        context.processingService.process(deliveryId);

        assertThatThrownBy(() -> context.cancelService.cancel(CLIENT, id))
                .isInstanceOf(NotificationNotCancellableException.class);
    }

    @Test
    void otherClientSeesNotFound() {
        NotificationId id = accept();

        assertThatThrownBy(() -> context.cancelService.cancel(new ClientId("other"), id))
                .isInstanceOf(NotificationNotFoundException.class);
        assertThatThrownBy(() -> context.queryService.get(new ClientId("other"), id))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
