package kz.company.notification.application.service;

import kz.company.notification.application.port.in.CancelNotificationUseCase;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Отмена уведомления. Осознанное исключение из правила «один агрегат — одна транзакция»:
 * отмена уведомления и его доставок атомарна (docs/adr/0006-aggregate-transactions.md).
 */
public class CancelNotificationService implements CancelNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final DeliveryRepository deliveryRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public CancelNotificationService(NotificationRepository notificationRepository,
                                     DeliveryRepository deliveryRepository,
                                     DomainEventPublisher eventPublisher,
                                     Clock clock) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public NotificationView cancel(ClientId clientId, NotificationId notificationId) {
        Instant now = clock.instant();
        Notification notification = NotificationQueryService.loadOwned(notificationRepository, clientId, notificationId);
        List<Delivery> deliveries = deliveryRepository.findByNotificationId(notificationId);

        // Сначала актуализируем статус: если воркер уже начал отправку, отмена невозможна.
        notification.refreshStatus(deliveries.stream().map(Delivery::status).toList(), now);
        boolean cancelled = notification.cancel(now);
        if (!cancelled) {
            return NotificationViewAssembler.toView(notification, deliveries);
        }
        deliveries.forEach(delivery -> delivery.cancel(now));

        List<DomainEvent> events = new ArrayList<>(notification.pullDomainEvents());
        deliveries.forEach(delivery -> events.addAll(delivery.pullDomainEvents()));
        Notification saved = notificationRepository.save(notification);
        List<Delivery> savedDeliveries = deliveryRepository.saveAll(deliveries);
        eventPublisher.publish(events);
        return NotificationViewAssembler.toView(saved, savedDeliveries);
    }
}
