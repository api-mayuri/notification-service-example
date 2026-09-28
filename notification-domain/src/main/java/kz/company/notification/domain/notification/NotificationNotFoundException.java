package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(NotificationId id) {
        super("NOTIFICATION_NOT_FOUND", DomainErrorKind.NOT_FOUND, "Notification " + id + " not found");
    }
}
