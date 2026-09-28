package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;

/**
 * Отправитель уведомления: {@code client_id} из JWT для REST или {@code ce_source} для Kafka.
 * Вместе с ключом идемпотентности образует уникальную пару.
 */
public record ClientId(String value) {

    public static final int MAX_LENGTH = 200;

    public ClientId {
        Guard.notBlank(value, "clientId");
        Guard.maxLength(value, MAX_LENGTH, "clientId");
    }

    @Override
    public String toString() {
        return value;
    }
}
