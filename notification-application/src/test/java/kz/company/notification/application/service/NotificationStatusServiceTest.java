package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.event.NotificationCompleted;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static kz.company.notification.application.ApplicationTestContext.CLIENT;
import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    @Test
    void statusFollowsDeliveries() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.givenActiveTemplate(ChannelType.SMS, "ru", null);
        NotificationId id = context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL,
                ChannelType.SMS)).notificationId();
        context.sms.willReturn(new SendResult.PermanentFailure("invalid number"));
        context.dispatcher.dispatched().forEach(context.processingService::process);

        context.statusService.synchronize(id);
        context.statusService.synchronize(id);

        NotificationView view = context.queryService.get(CLIENT, id);
        assertThat(view.status()).isEqualTo("PARTIALLY_FAILED");
        assertThat(view.deliveries()).hasSize(2);
        assertThat(view.deliveries().getFirst().channel()).isEqualTo("EMAIL");
        assertThat(view.deliveries().getFirst().maskedAddress()).isEqualTo("u***r@example.com");
        assertThat(view.templateKey()).isEqualTo("order-shipped");
        assertThat(context.events.eventsOf(NotificationCompleted.class)).hasSize(1);
    }

    @Test
    void unknownNotificationIsIgnored() {
        context.statusService.synchronize(new NotificationId(UUID.randomUUID()));

        assertThat(context.events.events()).isEmpty();
    }
}
