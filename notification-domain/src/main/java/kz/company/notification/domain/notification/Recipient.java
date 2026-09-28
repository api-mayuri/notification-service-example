package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;

/** Получатель: идентификатор и (необязательно) контакты, переданные в запросе. */
public record Recipient(RecipientId id, RecipientContacts contacts) {

    public Recipient {
        Guard.notNull(id, "recipientId");
        contacts = contacts == null ? RecipientContacts.NONE : contacts;
    }
}
