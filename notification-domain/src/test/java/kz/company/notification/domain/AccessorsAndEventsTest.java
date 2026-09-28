package kz.company.notification.domain;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.event.DeliveryConfirmed;
import kz.company.notification.domain.delivery.event.DeliveryFailed;
import kz.company.notification.domain.delivery.event.DeliveryRetryScheduled;
import kz.company.notification.domain.delivery.event.DeliveryScheduled;
import kz.company.notification.domain.delivery.event.DeliverySent;
import kz.company.notification.domain.delivery.event.DeliverySkipped;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.LocaleTag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static kz.company.notification.domain.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/** Аксессоры агрегатов и статусы событий, которые читают адаптеры. */
class AccessorsAndEventsTest {

    @Test
    void deliveryExposesItsState() {
        Delivery delivery = TestFixtures.pendingEmailDelivery();
        delivery.startAttempt(NOW);
        delivery.recordResult(SendResult.Success.real("id-1"), TestFixtures.retryPolicy(), NOW);

        assertThat(delivery.category()).isEqualTo(Category.TRANSACTIONAL);
        assertThat(delivery.priority()).isEqualTo(Priority.NORMAL);
        assertThat(delivery.recipientId().value()).isEqualTo("user-1");
        assertThat(delivery.channel()).isEqualTo(ChannelType.EMAIL);
        assertThat(delivery.createdAt()).isEqualTo(NOW);
        assertThat(delivery.updatedAt()).isEqualTo(NOW);
        assertThat(delivery.version()).isNull();
        assertThat(delivery.skipReason()).isEmpty();
        assertThat(delivery.message()).isPresent();
        assertThat(delivery.notificationId()).isNotNull();
        assertThat(DeliveryId.of(delivery.id().toString())).isEqualTo(delivery.id());
    }

    @Test
    void notificationExposesItsRequest() {
        Notification notification = TestFixtures.notification();

        assertThat(notification.priority()).isEqualTo(Priority.NORMAL);
        assertThat(notification.metadata()).isEmpty();
        assertThat(notification.updatedAt()).isEqualTo(NOW);
        assertThat(notification.version()).isNull();
        assertThat(notification.recipientId().toString()).isEqualTo("user-1");
        assertThat(notification.clientId().toString()).isEqualTo("orders-service");
        assertThat(notification.request().optionalIdempotencyKey().orElseThrow().toString()).isEqualTo("key-1");
        assertThat(notification.id().toString()).isEqualTo(notification.id().value().toString());
    }

    @Test
    void templateExposesItsState() {
        TemplateId id = new TemplateId(UUID.randomUUID());
        Template template = Template.create(id, new TemplateKey("welcome"), ChannelType.SMS, LocaleTag.of("kk"),
                new TemplateContentDefinition(null, "body", null, Set.of()), null, NOW);

        assertThat(template.id()).isEqualTo(id);
        assertThat(id.toString()).isEqualTo(id.value().toString());
        assertThat(template.createdAt()).isEqualTo(NOW);
        assertThat(template.updatedAt()).isEqualTo(NOW);
        assertThat(template.version()).isNull();
        assertThat(template.deduplicationWindow()).isEmpty();
        assertThat(template.key().toString()).isEqualTo("welcome");
        assertThat(template.locale().toLocale().getLanguage()).isEqualTo("kk");
    }

    @Test
    void deliveryEventsReportTheirStatus() {
        UUID delivery = UUID.randomUUID();
        UUID notification = UUID.randomUUID();
        Instant at = Instant.EPOCH;

        assertThat(List.of(
                new DeliveryScheduled(delivery, notification, "u", "EMAIL", "NORMAL", at, at).status(),
                new DeliverySent(delivery, notification, "EMAIL", 1, "p", false, at).status(),
                new DeliveryRetryScheduled(delivery, notification, "EMAIL", 1, at, "r", at).status(),
                new DeliveryFailed(delivery, notification, "EMAIL", 3, "r", true, at).status(),
                new DeliverySkipped(delivery, notification, "EMAIL", "NO_CONTACT", at).status(),
                new DeliveryConfirmed(delivery, notification, "EMAIL", at).status()))
                .containsExactly("PENDING", "SENT", "RETRY_SCHEDULED", "FAILED", "SKIPPED", "DELIVERED");
    }
}
