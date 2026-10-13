package kz.company.notification.infrastructure.in.events;

import kz.company.notification.application.port.in.SynchronizeNotificationStatusUseCase;
import kz.company.notification.domain.delivery.event.DeliveryStatusChanged;
import kz.company.notification.domain.notification.NotificationId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Пересчёт статуса уведомления после изменения статуса доставки: асинхронно, после коммита.
 * Публикация хранится в Event Publication Registry, поэтому при сбое будет повторена.
 *
 * <p>Events of sibling deliveries race on the same notification row; the loser retries
 * in a fresh transaction (the use case opens it) instead of waiting for outbox resubmission.
 */
@Component
public class NotificationStatusListener {

    private final SynchronizeNotificationStatusUseCase synchronizeNotificationStatus;

    public NotificationStatusListener(SynchronizeNotificationStatusUseCase synchronizeNotificationStatus) {
        this.synchronizeNotificationStatus = synchronizeNotificationStatus;
    }

    @Async
    @TransactionalEventListener
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 5, delay = 50, jitter = 50,
            multiplier = 2)
    public void on(DeliveryStatusChanged event) {
        synchronizeNotificationStatus.synchronize(new NotificationId(event.notificationId()));
    }
}
