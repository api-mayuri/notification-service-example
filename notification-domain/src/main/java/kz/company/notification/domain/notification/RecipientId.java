package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;

/** Идентификатор получателя во внешней системе пользователей. */
public record RecipientId(String value) {

    public static final int MAX_LENGTH = 100;

    public RecipientId {
        Guard.notBlank(value, "recipientId");
        Guard.maxLength(value, MAX_LENGTH, "recipientId");
    }

    @Override
    public String toString() {
        return value;
    }
}
