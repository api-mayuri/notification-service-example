package kz.company.notification.application.port.in;

import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.NotificationId;

/**
 * Отменить уведомление, пока ни одна доставка не началась. Повторная отмена ничего не делает.
 *
 * @throws kz.company.notification.domain.notification.NotificationNotCancellableException отправка уже началась
 * @throws kz.company.notification.domain.notification.NotificationNotFoundException нет уведомления этого отправителя
 */
public interface CancelNotificationUseCase {

    NotificationView cancel(ClientId clientId, NotificationId notificationId);
}
