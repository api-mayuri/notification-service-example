package kz.company.notification.application.service;

import kz.company.notification.application.port.in.SynchronizeNotificationStatusUseCase;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Пересчёт статуса уведомления после изменения статуса доставки. Вызывается асинхронно
 * по событию доставки в отдельной транзакции (eventual consistency между агрегатами).
 */
public class NotificationStatusService implements SynchronizeNotificationStatusUseCase {

    private final NotificationRepository notificationRepository;
    private final DeliveryRepository deliveryRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public NotificationStatusService(NotificationRepository notificationRepository,
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
    public void synchronize(NotificationId notificationId) {
        Optional<Notification> found = notificationRepository.findById(notificationId);
        if (found.isEmpty()) {
            return;
        }
        Notification notification = found.get();
        List<Delivery> deliveries = deliveryRepository.findByNotificationId(notificationId);
        if (notification.refreshStatus(deliveries.stream().map(Delivery::status).toList(), clock.instant())) {
            List<DomainEvent> events = notification.pullDomainEvents();
            notificationRepository.save(notification);
            eventPublisher.publish(events);
        }
    }
}
