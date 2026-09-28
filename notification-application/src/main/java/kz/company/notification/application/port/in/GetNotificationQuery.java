package kz.company.notification.application.port.in;

import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.NotificationId;

/** Статус уведомления с разбивкой по доставкам. Отправитель видит только свои уведомления. */
public interface GetNotificationQuery {

    NotificationView get(ClientId clientId, NotificationId notificationId);
}
