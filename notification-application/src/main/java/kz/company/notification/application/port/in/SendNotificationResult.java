package kz.company.notification.application.port.in;

import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationStatus;

/**
 * Результат приёма уведомления.
 *
 * @param outcome что произошло с запросом: создано новое, повтор по ключу идемпотентности
 *                или схлопнуто с недавним одинаковым уведомлением
 */
public record SendNotificationResult(NotificationId notificationId, NotificationStatus status, Outcome outcome) {

    public enum Outcome {
        CREATED,
        REPLAYED,
        DEDUPLICATED
    }
}
