package kz.company.notification.application.port.out;

import kz.company.notification.domain.delivery.Delivery;

/**
 * Поставить доставку в очередь канала. Вызывается внутри транзакции сценария; реализация
 * обязана идти через outbox, чтобы постановка и изменение в БД были атомарны.
 */
public interface DeliveryDispatcher {

    void dispatch(Delivery delivery);
}
