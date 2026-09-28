package kz.company.notification.application.port.in;

import kz.company.notification.domain.notification.NotificationId;

/** Пересчитать статус уведомления по статусам доставок (после изменения статуса доставки). */
public interface SynchronizeNotificationStatusUseCase {

    void synchronize(NotificationId notificationId);
}
