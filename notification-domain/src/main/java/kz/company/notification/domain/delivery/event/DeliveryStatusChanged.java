package kz.company.notification.domain.delivery.event;

import kz.company.notification.domain.common.DomainEvent;

import java.util.UUID;

/**
 * Общий контракт событий об изменении статуса доставки. По ним пересчитывается статус
 * уведомления и публикуются события наружу.
 */
public interface DeliveryStatusChanged extends DomainEvent {

    UUID deliveryId();

    UUID notificationId();

    String channel();

    /** Новый статус доставки ({@code DeliveryStatus#name()}). */
    String status();
}
