package kz.company.notification.domain.delivery;

import kz.company.notification.domain.TestFixtures;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.event.DeliveryCancelled;
import kz.company.notification.domain.delivery.event.DeliveryConfirmed;
import kz.company.notification.domain.delivery.event.DeliveryFailed;
import kz.company.notification.domain.delivery.event.DeliveryRetryScheduled;
import kz.company.notification.domain.delivery.event.DeliveryScheduled;
import kz.company.notification.domain.delivery.event.DeliverySent;
import kz.company.notification.domain.delivery.event.DeliverySkipped;
import kz.company.notification.domain.notification.Notification;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static kz.company.notification.domain.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryTest {

    @Test
    void newDeliveryIsPendingAndRegistersScheduledEvent() {
        Notification notification = TestFixtures.notification();
        DeliveryFactory factory = new DeliveryFactory(() -> new DeliveryId(UUID.randomUUID()));

        Delivery delivery = factory.create(notification, List.of(new DeliveryPlan.Deliverable(ChannelType.EMAIL,
                new EmailAddress("user@example.com"), TestFixtures.message(), NOW)), NOW).getFirst();

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.attempts()).isEqualTo(0);
        assertThat(delivery.isDue(NOW)).isTrue();
        List<DomainEvent> events = delivery.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(DeliveryScheduled.class);
        assertThat(delivery.pullDomainEvents()).isEmpty();
    }

    @Test
    void skippedPlanCreatesSkippedDelivery() {
        Notification notification = TestFixtures.notification();
        DeliveryFactory factory = new DeliveryFactory(() -> new DeliveryId(UUID.randomUUID()));

        Delivery delivery = factory.create(notification,
                List.of(new DeliveryPlan.Skip(ChannelType.SMS, SkipReason.NO_CONTACT)), NOW).getFirst();

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.SKIPPED);
        assertThat(delivery.skipReason()).contains(SkipReason.NO_CONTACT);
        assertThat(delivery.address()).isEmpty();
        assertThat(delivery.pullDomainEvents().getFirst()).isInstanceOf(DeliverySkipped.class);
    }

    @Test
    void successfulAttemptMarksSent() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.markDispatched(NOW);

        delivery.startAttempt(NOW);
        delivery.recordResult(SendResult.Success.real("msg-1"), TestFixtures.retryPolicy(), NOW.plusSeconds(1));

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.attempts()).isEqualTo(1);
        assertThat(delivery.providerMessageId()).contains("msg-1");
        assertThat(delivery.simulated()).isFalse();
        assertThat(delivery.sentAt()).contains(NOW.plusSeconds(1));
        assertThat(delivery.pullDomainEvents().getFirst()).isInstanceOf(DeliverySent.class);
    }

    @Test
    void simulatedSuccessIsFlagged() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        delivery.recordResult(SendResult.Success.simulated("fake-1"), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.simulated()).isTrue();
        DeliverySent event = (DeliverySent) delivery.pullDomainEvents().getFirst();
        assertThat(event.simulated()).isTrue();
    }

    @Test
    void retryableFailureSchedulesRetryWithBackoff() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        delivery.recordResult(new SendResult.RetryableFailure("timeout"), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.RETRY_SCHEDULED);
        assertThat(delivery.nextAttemptAt()).contains(NOW.plusSeconds(1));
        assertThat(delivery.lastError()).contains("timeout");
        DeliveryRetryScheduled event = (DeliveryRetryScheduled) delivery.pullDomainEvents().getFirst();
        assertThat(event.attempt()).isEqualTo(1);
    }

    @Test
    void retryableFailureAfterLastAttemptFails() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        for (int attempt = 1; attempt <= 2; attempt++) {
            delivery.startAttempt(NOW);
            delivery.recordResult(new SendResult.RetryableFailure("timeout"), TestFixtures.retryPolicy(), NOW);
        }
        delivery.startAttempt(NOW);

        delivery.recordResult(new SendResult.RetryableFailure("timeout"), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.attempts()).isEqualTo(3);
        List<DomainEvent> events = delivery.pullDomainEvents();
        DeliveryFailed failed = (DeliveryFailed) events.getLast();
        assertThat(failed.permanent()).isFalse();
        assertThat(failed.reason()).contains("Attempts exhausted");
    }

    @Test
    void permanentFailureFailsImmediately() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        delivery.recordResult(new SendResult.PermanentFailure("550 mailbox unavailable"), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.FAILED);
        DeliveryFailed failed = (DeliveryFailed) delivery.pullDomainEvents().getFirst();
        assertThat(failed.permanent()).isTrue();
    }

    @Test
    void sentDeliveryCannotBeSentAgain() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);
        delivery.recordResult(SendResult.Success.real("msg-1"), TestFixtures.retryPolicy(), NOW);

        assertThatThrownBy(() -> delivery.startAttempt(NOW))
                .isInstanceOf(InvalidDeliveryStatusTransitionException.class)
                .hasMessageContaining("SENT");
    }

    @Test
    void recordResultRequiresSendingState() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();

        assertThatThrownBy(() -> delivery.recordResult(SendResult.Success.real("x"), TestFixtures.retryPolicy(), NOW))
                .isInstanceOf(InvalidDeliveryStatusTransitionException.class);
    }

    @Test
    void sentDeliveryCanBeConfirmed() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);
        delivery.recordResult(SendResult.Success.real("msg-1"), TestFixtures.retryPolicy(), NOW);
        delivery.pullDomainEvents();

        delivery.confirmDelivered(NOW);

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.pullDomainEvents().getFirst()).isInstanceOf(DeliveryConfirmed.class);
    }

    @Test
    void pendingDeliveryCanBeCancelledOnce() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();

        assertThat(delivery.cancel(NOW)).isTrue();
        assertThat(delivery.cancel(NOW)).isFalse();

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.CANCELLED);
        assertThat(delivery.pullDomainEvents()).hasSize(1);
    }

    @Test
    void sendingDeliveryIsNotCancelled() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        assertThat(delivery.cancel(NOW)).isFalse();
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.SENDING);
    }

    @Test
    void cancelledEventCarriesChannel() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.cancel(NOW);

        DeliveryCancelled event = (DeliveryCancelled) delivery.pullDomainEvents().getFirst();

        assertThat(event.channel()).isEqualTo("EMAIL");
        assertThat(event.status()).isEqualTo("CANCELLED");
    }

    @Test
    void staleSendingAttemptIsRequeued() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.markDispatched(NOW);
        delivery.startAttempt(NOW);

        delivery.recoverStaleAttempt(TestFixtures.retryPolicy(), NOW.plusSeconds(600));

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.RETRY_SCHEDULED);
        assertThat(delivery.dispatchedAt()).isEmpty();
        assertThat(delivery.isDue(NOW.plusSeconds(600))).isTrue();
    }

    @Test
    void staleSendingAttemptWithoutAttemptsLeftFails() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        for (int attempt = 1; attempt <= 2; attempt++) {
            delivery.startAttempt(NOW);
            delivery.recordResult(new SendResult.RetryableFailure("timeout"), TestFixtures.retryPolicy(), NOW);
        }
        delivery.startAttempt(NOW);

        delivery.recoverStaleAttempt(TestFixtures.retryPolicy(), NOW.plusSeconds(600));

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void recoverStaleRequiresSendingState() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();

        assertThatThrownBy(() -> delivery.recoverStaleAttempt(TestFixtures.retryPolicy(), NOW))
                .isInstanceOf(InvalidDeliveryStatusTransitionException.class);
    }

    @Test
    void deferKeepsStatusAndClearsDispatch() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.markDispatched(NOW);

        delivery.defer(NOW.plus(Duration.ofMinutes(30)), "rate limit", NOW);

        assertThat(delivery.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.dispatchedAt()).isEmpty();
        assertThat(delivery.isDue(NOW)).isFalse();
        assertThat(delivery.isDue(NOW.plus(Duration.ofMinutes(30)))).isTrue();
        assertThat(delivery.attempts()).isEqualTo(0);
    }

    @Test
    void requeueMakesDeliveryDueAgain() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);
        delivery.recordResult(new SendResult.RetryableFailure("timeout"), TestFixtures.retryPolicy(), NOW);
        delivery.markDispatched(NOW);

        delivery.requeue(NOW.plusSeconds(3600));

        assertThat(delivery.isDue(NOW.plusSeconds(3600))).isTrue();
    }

    @Test
    void dispatchIsRejectedForSentDelivery() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);
        delivery.recordResult(SendResult.Success.real("id"), TestFixtures.retryPolicy(), NOW);

        assertThatThrownBy(() -> delivery.markDispatched(NOW))
                .isInstanceOf(InvalidDeliveryStatusTransitionException.class)
                .hasMessageContaining("dispatch");
    }

    @Test
    void parkingFailsUnfinishedDeliveryOnlyOnce() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();

        assertThat(delivery.park("dead letter", NOW)).isTrue();
        assertThat(delivery.park("dead letter", NOW)).isFalse();
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void snapshotRoundTripPreservesState() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        Delivery restored = Delivery.restore(delivery.snapshot());

        assertThat(restored.snapshot()).isEqualTo(delivery.snapshot());
        assertThat(restored.pullDomainEvents()).isEmpty();
    }

    @Test
    void longErrorIsTruncated() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);

        delivery.recordResult(new SendResult.PermanentFailure("x".repeat(5000)), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.lastError().orElseThrow().length()).isEqualTo(1000);
    }
}
