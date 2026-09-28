package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.Category;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;

class DeliverySchedulingServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    @Test
    void dispatchesScheduledDeliveryWhenDue() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        SendNotificationCommand base = command("k", Category.TRANSACTIONAL, ChannelType.EMAIL);
        context.sendService.send(new SendNotificationCommand(base.clientId(), base.idempotencyKey(), base.recipient(),
                base.content(), base.channels(), base.category(), base.priority(),
                context.clock.instant().plus(Duration.ofHours(1)), base.metadata(), false));

        assertThat(context.schedulingService.dispatchDue()).isEqualTo(0);
        context.clock.advance(Duration.ofHours(1));
        assertThat(context.schedulingService.dispatchDue()).isEqualTo(1);
        assertThat(context.schedulingService.dispatchDue()).isEqualTo(0);

        assertThat(context.dispatcher.dispatched()).hasSize(1);
    }

    @Test
    void staleSendingDeliveryIsReturnedToQueue() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL));
        Delivery delivery = context.deliveries.all().getFirst();
        delivery.startAttempt(context.clock.instant());
        context.deliveries.save(delivery);

        context.clock.advance(Duration.ofMinutes(6));
        assertThat(context.schedulingService.recover()).isEqualTo(1);

        Delivery recovered = context.deliveries.findById(delivery.id()).orElseThrow();
        assertThat(recovered.status()).isEqualTo(DeliveryStatus.RETRY_SCHEDULED);
        context.dispatcher.clear();
        assertThat(context.schedulingService.dispatchDue()).isEqualTo(1);
    }

    @Test
    void overdueRetryIsRequeued() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.sendService.send(command("k", Category.TRANSACTIONAL, ChannelType.EMAIL));
        DeliveryId id = context.dispatcher.dispatched().getFirst();
        context.email.willReturn(new SendResult.RetryableFailure("timeout"));
        context.processingService.process(id);

        context.clock.advance(Duration.ofMinutes(5));
        assertThat(context.schedulingService.recover()).isEqualTo(0);
        context.clock.advance(Duration.ofMinutes(6));
        assertThat(context.schedulingService.recover()).isEqualTo(1);

        assertThat(context.deliveries.findById(id).orElseThrow().isDue(context.clock.instant())).isTrue();
    }
}
