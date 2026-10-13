package kz.company.notification.domain.delivery;

import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.RecipientId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Хранилище агрегатов {@link Delivery}. */
public interface DeliveryRepository {

    /** Сохранить с проверкой версии; возвращает агрегат с новой версией. */
    Delivery save(Delivery delivery);

    List<Delivery> saveAll(List<Delivery> deliveries);

    Optional<Delivery> findById(DeliveryId id);

    List<Delivery> findByNotificationId(NotificationId notificationId);

    /**
     * Доставки, которые пора поставить в топик: ждут отправки, не поставлены и время подошло.
     * Строки блокируются до конца транзакции; строки, заблокированные другим экземпляром,
     * пропускаются ({@code FOR UPDATE SKIP LOCKED}).
     */
    List<Delivery> lockDueForDispatch(Instant now, int limit);

    /** Доставки в SENDING, не обновлявшиеся с {@code updatedBefore}. Блокируются как выше. */
    List<Delivery> lockStaleSending(Instant updatedBefore, int limit);

    /**
     * Доставки в PENDING или RETRY_SCHEDULED, поставленные в топик раньше {@code dueBefore},
     * чья попытка должна была случиться раньше {@code dueBefore} (сообщение потеряно или
     * ушло в DLT нечитаемым). Блокируются как выше.
     */
    List<Delivery> lockOverdueDispatches(Instant dueBefore, int limit);

    /** Использование лимита частоты: отправки получателю по каналу начиная с {@code since}. */
    RateWindowUsage rateWindowUsage(RecipientId recipientId, ChannelType channel, Instant since);
}
