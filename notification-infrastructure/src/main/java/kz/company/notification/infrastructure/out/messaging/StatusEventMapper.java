package kz.company.notification.infrastructure.out.messaging;

import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.event.DeliveryStatusChanged;
import kz.company.notification.domain.notification.event.NotificationAccepted;
import kz.company.notification.domain.notification.event.NotificationCancelled;
import kz.company.notification.domain.notification.event.NotificationCompleted;
import kz.company.notification.infrastructure.messaging.ExternalMessage;
import kz.company.notification.infrastructure.messaging.MessageTypes;
import kz.company.notification.infrastructure.messaging.TopicNames;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Доменные события → события для внешних систем в {@code notification.events.v1} (ключ notificationId).
 *
 * <p>Типы: {@code kz.company.notification.delivery.<статус>.v1} (scheduled, sent, retry-scheduled,
 * failed, skipped, cancelled, delivered) и {@code kz.company.notification.notification.<событие>.v1}
 * (accepted, completed, cancelled). Персональные данные (адреса) в события не попадают.
 */
@Component
public class StatusEventMapper {

    private static final TypeReference<Map<String, Object>> FIELDS = new TypeReference<>() {
    };

    private final ExternalMessageFactory messageFactory;
    private final TopicNames topicNames;
    private final JsonMapper jsonMapper;

    public StatusEventMapper(ExternalMessageFactory messageFactory, TopicNames topicNames, JsonMapper jsonMapper) {
        this.messageFactory = messageFactory;
        this.topicNames = topicNames;
        this.jsonMapper = jsonMapper;
    }

    public Optional<ExternalMessage> toExternal(DomainEvent event) {
        return switch (event) {
            case DeliveryStatusChanged delivery -> Optional.of(messageFactory.create(topicNames.events(),
                    delivery.notificationId().toString(),
                    MessageTypes.event("delivery." + kebab(statusName(delivery))),
                    delivery.deliveryId().toString(), delivery.occurredAt(), withStatus(delivery)));
            case NotificationAccepted accepted -> Optional.of(messageFactory.create(topicNames.events(),
                    accepted.notificationId().toString(), MessageTypes.event("notification.accepted"),
                    accepted.notificationId().toString(), accepted.occurredAt(), accepted));
            case NotificationCompleted completed -> Optional.of(messageFactory.create(topicNames.events(),
                    completed.notificationId().toString(), MessageTypes.event("notification.completed"),
                    completed.notificationId().toString(), completed.occurredAt(), completed));
            case NotificationCancelled cancelled -> Optional.of(messageFactory.create(topicNames.events(),
                    cancelled.notificationId().toString(), MessageTypes.event("notification.cancelled"),
                    cancelled.notificationId().toString(), cancelled.occurredAt(), cancelled));
            default -> Optional.empty();
        };
    }

    /** Поля события и новый статус доставки (у record-события статус — метод, а не компонент). */
    private Map<String, Object> withStatus(DeliveryStatusChanged event) {
        Map<String, Object> fields = new LinkedHashMap<>(jsonMapper.convertValue(event, FIELDS));
        fields.put("status", event.status());
        return fields;
    }

    /** Имя статуса в событии: PENDING публикуется как «scheduled», DELIVERED — как «delivered». */
    private static String statusName(DeliveryStatusChanged event) {
        return "PENDING".equals(event.status()) ? "SCHEDULED" : event.status();
    }

    private static String kebab(String status) {
        return status.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
