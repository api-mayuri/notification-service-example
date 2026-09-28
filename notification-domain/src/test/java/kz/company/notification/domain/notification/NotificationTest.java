package kz.company.notification.domain.notification;

import kz.company.notification.domain.TestFixtures;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.notification.event.NotificationAccepted;
import kz.company.notification.domain.notification.event.NotificationCancelled;
import kz.company.notification.domain.notification.event.NotificationCompleted;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static kz.company.notification.domain.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void acceptedNotificationRegistersEvent() {
        Notification notification = TestFixtures.notification();

        assertThat(notification.status()).isEqualTo(NotificationStatus.ACCEPTED);
        List<DomainEvent> events = notification.pullDomainEvents();
        assertThat(events).hasSize(1);
        NotificationAccepted accepted = (NotificationAccepted) events.getFirst();
        assertThat(accepted.channels()).containsExactly("EMAIL");
        assertThat(accepted.clientId()).isEqualTo("orders-service");
    }

    @Test
    void refreshStatusCompletesAndRegistersEventOnce() {
        Notification notification = TestFixtures.notification();
        notification.pullDomainEvents();

        assertThat(notification.refreshStatus(List.of(DeliveryStatus.SENDING), NOW)).isTrue();
        assertThat(notification.status()).isEqualTo(NotificationStatus.IN_PROGRESS);
        assertThat(notification.pullDomainEvents()).isEmpty();

        assertThat(notification.refreshStatus(List.of(DeliveryStatus.SENT), NOW)).isTrue();
        assertThat(notification.refreshStatus(List.of(DeliveryStatus.SENT), NOW)).isFalse();
        List<DomainEvent> events = notification.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(((NotificationCompleted) events.getFirst()).status()).isEqualTo("COMPLETED");
    }

    @Test
    void acceptedNotificationCanBeCancelledIdempotently() {
        Notification notification = TestFixtures.notification();
        notification.pullDomainEvents();

        assertThat(notification.cancel(NOW)).isTrue();
        assertThat(notification.cancel(NOW)).isFalse();

        assertThat(notification.status()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(notification.pullDomainEvents().getFirst()).isInstanceOf(NotificationCancelled.class);
        assertThat(notification.refreshStatus(List.of(DeliveryStatus.SENT), NOW)).isFalse();
    }

    @Test
    void startedNotificationCannotBeCancelled() {
        Notification notification = TestFixtures.notification();
        notification.refreshStatus(List.of(DeliveryStatus.SENDING), NOW);

        assertThatThrownBy(() -> notification.cancel(NOW))
                .isInstanceOf(NotificationNotCancellableException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void fingerprintComparison() {
        Notification notification = TestFixtures.notification();

        assertThat(notification.hasSameRequestAs("fingerprint")).isTrue();
        assertThat(notification.hasSameRequestAs("other")).isFalse();
    }

    @Test
    void snapshotRoundTrip() {
        Notification notification = TestFixtures.notification();

        Notification restored = Notification.restore(notification.snapshot());

        assertThat(restored.snapshot()).isEqualTo(notification.snapshot());
        assertThat(restored.pullDomainEvents()).isEmpty();
        assertThat(restored.deduplicationKey()).isEmpty();
        assertThat(restored.requestedChannels()).containsExactly(ChannelType.EMAIL);
    }

    @Test
    void requestRequiresChannelsAndLimitsMetadata() {
        assertThatThrownBy(() -> TestFixtures.request(Category.MARKETING))
                .isInstanceOf(DomainValidationException.class);

        Map<String, String> tooMuch = new java.util.HashMap<>();
        for (int index = 0; index <= NotificationRequest.MAX_METADATA_ENTRIES; index++) {
            tooMuch.put("key" + index, "value");
        }
        assertThatThrownBy(() -> new NotificationRequest(new ClientId("c"), null, null,
                new Recipient(new RecipientId("r"), null), RawContent.class.cast(new RawContent(null, "t", null)),
                Set.of(ChannelType.SMS), Category.SYSTEM, null, null, tooMuch))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void requestDefaultsPriorityToNormal() {
        NotificationRequest request = new NotificationRequest(new ClientId("c"), null, null,
                new Recipient(new RecipientId("r"), null), new RawContent(null, "t", null),
                Set.of(ChannelType.SMS), Category.SYSTEM, null, null, null);

        assertThat(request.priority()).isEqualTo(Priority.NORMAL);
        assertThat(request.optionalIdempotencyKey()).isEmpty();
        assertThat(request.optionalSendAt()).isEmpty();
        assertThat(request.recipient().contacts().isEmpty()).isTrue();
    }

    @Test
    void identifiersValidateInput() {
        UUID uuid = UUID.randomUUID();
        assertThat(NotificationId.of(uuid.toString()).value()).isEqualTo(uuid);
        assertThatThrownBy(() -> new ClientId(" ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RecipientId("x".repeat(101))).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new IdempotencyKey("with space")).isInstanceOf(DomainValidationException.class);
        assertThat(new IdempotencyKey("batch-1").forBatchItem(3).value()).isEqualTo("batch-1:3");
    }
}
