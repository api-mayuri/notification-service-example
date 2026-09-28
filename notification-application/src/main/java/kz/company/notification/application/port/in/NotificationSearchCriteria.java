package kz.company.notification.application.port.in;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.NotificationStatus;
import kz.company.notification.domain.notification.RecipientId;

import java.time.Instant;

/**
 * Фильтры поиска. {@code null} в необязательном поле — фильтр не применяется.
 * Отправитель всегда ищет только среди своих уведомлений.
 */
public record NotificationSearchCriteria(
        ClientId clientId,
        RecipientId recipientId,
        NotificationStatus status,
        ChannelType channel,
        Instant from,
        Instant to) {

    public NotificationSearchCriteria {
        Guard.notNull(clientId, "clientId");
        if (from != null && to != null && from.isAfter(to)) {
            throw new DomainValidationException("INVALID_PERIOD", "'from' must not be after 'to'");
        }
    }
}
