package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;

import java.util.UUID;

/** Идентификатор уведомления. */
public record NotificationId(UUID value) {

    public NotificationId {
        Guard.notNull(value, "notificationId");
    }

    public static NotificationId of(String value) {
        return new NotificationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
