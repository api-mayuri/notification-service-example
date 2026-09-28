package kz.company.notification.domain;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryFactory;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryPlan;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.policy.RetryPolicy;
import kz.company.notification.domain.template.TemplateKey;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Общие тестовые данные доменных тестов. */
public final class TestFixtures {

    public static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    private TestFixtures() {
    }

    public static NotificationRequest request(Category category, ChannelType... channels) {
        return new NotificationRequest(new ClientId("orders-service"), new IdempotencyKey("key-1"), "fingerprint",
                new Recipient(new RecipientId("user-1"), RecipientContacts.NONE),
                new TemplateContent(new TemplateKey("order-shipped"), null, Map.of("orderId", "ORD-42")),
                Set.of(channels), category, Priority.NORMAL, null, Map.of());
    }

    public static Notification notification() {
        return Notification.accept(new NotificationId(UUID.randomUUID()),
                request(Category.TRANSACTIONAL, ChannelType.EMAIL), null, NOW);
    }

    public static RenderedMessage message() {
        return new RenderedMessage("Subject", "Text", "<p>Html</p>", "order-shipped", 1, "ru");
    }

    public static Delivery pendingEmailDelivery() {
        Notification notification = notification();
        DeliveryFactory factory = new DeliveryFactory(() -> new DeliveryId(UUID.randomUUID()));
        Delivery delivery = factory.create(notification, List.of(new DeliveryPlan.Deliverable(ChannelType.EMAIL,
                new EmailAddress("user@example.com"), message(), NOW)), NOW).getFirst();
        delivery.pullDomainEvents();
        return delivery;
    }

    /** Политика без разброса: 1с, 2с, 4с…, максимум 3 попытки. */
    public static RetryPolicy retryPolicy() {
        return new RetryPolicy(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30), 0.0, () -> 0.5);
    }
}
