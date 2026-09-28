package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

/** Отмена невозможна: отправка уже началась или завершилась. */
public class NotificationNotCancellableException extends DomainException {

    public NotificationNotCancellableException(NotificationId id, NotificationStatus status) {
        super("NOTIFICATION_NOT_CANCELLABLE", DomainErrorKind.CONFLICT,
                "Notification " + id + " cannot be cancelled in status " + status);
    }
}
