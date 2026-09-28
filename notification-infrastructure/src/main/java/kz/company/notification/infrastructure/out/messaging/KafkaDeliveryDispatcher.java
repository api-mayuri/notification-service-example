package kz.company.notification.infrastructure.out.messaging;

import kz.company.notification.application.port.out.DeliveryDispatcher;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.infrastructure.messaging.MessageTypes;
import kz.company.notification.infrastructure.messaging.TopicNames;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Постановка доставки в топик канала через outbox: сообщение публикуется как событие приложения
 * в текущей транзакции, Spring Modulith отправляет его в Kafka после коммита. Ключ — recipientId,
 * чтобы сообщения одного получателя шли по порядку в одной партиции.
 */
@Component
public class KafkaDeliveryDispatcher implements DeliveryDispatcher {

    private final ApplicationEventPublisher eventPublisher;
    private final ExternalMessageFactory messageFactory;
    private final TopicNames topicNames;
    private final Clock clock;

    public KafkaDeliveryDispatcher(ApplicationEventPublisher eventPublisher, ExternalMessageFactory messageFactory,
                                   TopicNames topicNames, Clock clock) {
        this.eventPublisher = eventPublisher;
        this.messageFactory = messageFactory;
        this.topicNames = topicNames;
        this.clock = clock;
    }

    @Override
    public void dispatch(Delivery delivery) {
        DeliveryRequestPayload payload = new DeliveryRequestPayload(delivery.id().value(),
                delivery.notificationId().value(), delivery.channel().code(), delivery.priority().name(),
                delivery.attempts());
        eventPublisher.publishEvent(messageFactory.create(
                topicNames.delivery(delivery.channel(), delivery.priority()),
                delivery.recipientId().value(),
                MessageTypes.DELIVERY_REQUESTED,
                delivery.id().toString(),
                clock.instant(),
                payload));
    }

    /** Сообщение в топике канала. Состояние доставки хранится в БД; здесь только ссылка на неё. */
    public record DeliveryRequestPayload(UUID deliveryId, UUID notificationId, String channel, String priority,
                                         int attemptsSoFar) {
    }
}
