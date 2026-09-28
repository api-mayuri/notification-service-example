package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.DeliveryId;

/**
 * Обработка доставки воркером канала. Идемпотентна: доставка в завершённом статусе
 * (SENT и далее) повторно не отправляется.
 */
public interface ProcessDeliveryUseCase {

    DeliveryOutcome process(DeliveryId deliveryId);

    /**
     * Перевести доставку в FAILED: сообщение попало в dead letter topic (parking lot).
     *
     * @return {@code true}, если статус изменён
     */
    boolean park(DeliveryId deliveryId, String reason);
}
