package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.DeliveryOutcome;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.event.DeliveryFailed;
import kz.company.notification.domain.delivery.event.DeliveryRetryScheduled;
import kz.company.notification.domain.delivery.event.DeliverySent;
import kz.company.notification.domain.notification.Category;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;

class DeliveryProcessingServiceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    private DeliveryId accept(ChannelType channel, Category category, String key) {
        context.givenActiveTemplateOnce(channel);
        context.sendService.send(command(key, category, channel));
        return context.dispatcher.dispatched().getLast();
    }

    @Test
    void successfulSendMarksDeliverySent() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");

        DeliveryOutcome outcome = context.processingService.process(id);

        assertThat(outcome).isInstanceOf(DeliveryOutcome.Sent.class);
        assertThat(context.deliveries.findById(id).orElseThrow().status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(context.events.eventsOf(DeliverySent.class)).hasSize(1);
        assertThat(context.email.sent()).hasSize(1);
    }

    @Test
    void repeatedMessageForSentDeliveryIsIgnored() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");
        context.processingService.process(id);

        DeliveryOutcome outcome = context.processingService.process(id);

        assertThat(outcome).isInstanceOf(DeliveryOutcome.Ignored.class);
        assertThat(context.email.sent()).hasSize(1);
    }

    @Test
    void temporaryFailureSchedulesRetryThenExhausts() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");
        context.email.willReturn(new SendResult.RetryableFailure("421 try later"))
                .willReturn(new IllegalStateException("connection reset"))
                .willReturn(new SendResult.RetryableFailure("421 try later"));

        DeliveryOutcome first = context.processingService.process(id);
        DeliveryOutcome second = context.processingService.process(id);
        DeliveryOutcome third = context.processingService.process(id);

        DeliveryOutcome.RetryScheduled retry = (DeliveryOutcome.RetryScheduled) first;
        assertThat(retry.nextAttemptAt()).isEqualTo(ApplicationTestContext.START.plusSeconds(10));
        assertThat(second).isInstanceOf(DeliveryOutcome.RetryScheduled.class);
        assertThat(((DeliveryOutcome.RetryScheduled) second).reason()).contains("connection reset");
        DeliveryOutcome.Failed failed = (DeliveryOutcome.Failed) third;
        assertThat(failed.permanent()).isFalse();
        assertThat(context.events.eventsOf(DeliveryRetryScheduled.class)).hasSize(2);
        assertThat(context.events.eventsOf(DeliveryFailed.class)).hasSize(1);
    }

    @Test
    void permanentFailureFailsWithoutRetry() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");
        context.email.willReturn(new SendResult.PermanentFailure("550 no such user"));

        DeliveryOutcome outcome = context.processingService.process(id);

        assertThat(outcome).isEqualTo(new DeliveryOutcome.Failed("550 no such user", true));
    }

    @Test
    void disabledChannelFailsPermanently() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");
        context.registry.unregister(ChannelType.EMAIL);

        DeliveryOutcome outcome = context.processingService.process(id);

        assertThat(((DeliveryOutcome.Failed) outcome).permanent()).isTrue();
    }

    @Test
    void rateLimitDefersMarketingSms() {
        DeliveryId first = accept(ChannelType.SMS, Category.MARKETING, "k1");
        DeliveryId second = accept(ChannelType.SMS, Category.MARKETING, "k2");
        DeliveryId third = accept(ChannelType.SMS, Category.MARKETING, "k3");
        context.processingService.process(first);
        context.clock.advance(Duration.ofMinutes(1));
        context.processingService.process(second);

        DeliveryOutcome outcome = context.processingService.process(third);

        assertThat(outcome).isEqualTo(new DeliveryOutcome.Deferred(ApplicationTestContext.START.plus(Duration.ofHours(1))));
        Delivery deferred = context.deliveries.findById(third).orElseThrow();
        assertThat(deferred.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(deferred.dispatchedAt()).isEmpty();
        assertThat(deferred.attempts()).isEqualTo(0);
    }

    @Test
    void rateLimitDoesNotApplyToTransactional() {
        DeliveryId first = accept(ChannelType.SMS, Category.TRANSACTIONAL, "k1");
        DeliveryId second = accept(ChannelType.SMS, Category.TRANSACTIONAL, "k2");
        DeliveryId third = accept(ChannelType.SMS, Category.TRANSACTIONAL, "k3");
        context.processingService.process(first);
        context.processingService.process(second);

        assertThat(context.processingService.process(third)).isInstanceOf(DeliveryOutcome.Sent.class);
    }

    @Test
    void unknownDeliveryIsIgnored() {
        assertThat(context.processingService.process(new DeliveryId(UUID.randomUUID())))
                .isInstanceOf(DeliveryOutcome.Ignored.class);
        assertThat(context.processingService.park(new DeliveryId(UUID.randomUUID()), "dlt")).isFalse();
    }

    @Test
    void parkingFailsUnfinishedDelivery() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");

        assertThat(context.processingService.park(id, "moved to DLT")).isTrue();
        assertThat(context.processingService.park(id, "moved to DLT")).isFalse();

        assertThat(context.deliveries.findById(id).orElseThrow().status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void deliveryRecoveredWhileSendingKeepsRecoveryState() {
        DeliveryId id = accept(ChannelType.EMAIL, Category.TRANSACTIONAL, "k1");
        context.email.willReturn((java.util.function.Supplier<SendResult>) () -> {
            context.clock.advance(Duration.ofMinutes(10));
            context.schedulingService.recover();
            return SendResult.Success.real("late");
        });

        DeliveryOutcome outcome = context.processingService.process(id);

        assertThat(outcome).isInstanceOf(DeliveryOutcome.Ignored.class);
        assertThat(context.deliveries.findById(id).orElseThrow().status()).isEqualTo(DeliveryStatus.RETRY_SCHEDULED);
    }
}
