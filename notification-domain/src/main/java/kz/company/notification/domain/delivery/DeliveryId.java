package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.Guard;

import java.util.UUID;

/** Идентификатор доставки. */
public record DeliveryId(UUID value) {

    public DeliveryId {
        Guard.notNull(value, "deliveryId");
    }

    public static DeliveryId of(String value) {
        return new DeliveryId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
