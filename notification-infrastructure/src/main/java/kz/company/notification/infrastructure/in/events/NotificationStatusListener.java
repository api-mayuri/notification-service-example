package kz.company.notification.infrastructure.in.events;

import kz.company.notification.application.port.in.SynchronizeNotificationStatusUseCase;
import kz.company.notification.domain.delivery.event.DeliveryStatusChanged;
import kz.company.notification.domain.notification.NotificationId;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Пересчёт статуса уведомления после изменения статуса доставки. {@code @ApplicationModuleListener}
 * — асинхронный слушатель в новой транзакции после коммита; публикация хранится в Event Publication
 * Registry, поэтому при сбое будет повторена.
 */
@Component
public class NotificationStatusListener {

    private final SynchronizeNotificationStatusUseCase synchronizeNotificationStatus;

    public NotificationStatusListener(SynchronizeNotificationStatusUseCase synchronizeNotificationStatus) {
        this.synchronizeNotificationStatus = synchronizeNotificationStatus;
    }

    @ApplicationModuleListener
    public void on(DeliveryStatusChanged event) {
        synchronizeNotificationStatus.synchronize(new NotificationId(event.notificationId()));
    }
}
