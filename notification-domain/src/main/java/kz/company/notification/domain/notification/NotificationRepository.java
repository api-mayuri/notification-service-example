package kz.company.notification.domain.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Хранилище агрегатов {@link Notification}. */
public interface NotificationRepository {

    /**
     * Сохранить агрегат. Реализация проверяет версию (оптимистическая блокировка)
     * и возвращает агрегат с новой версией.
     */
    Notification save(Notification notification);

    Optional<Notification> findById(NotificationId id);

    Optional<Notification> findByIdempotencyKey(ClientId clientId, IdempotencyKey key);

    /** Последнее уведомление с тем же ключом дедупликации, принятое не раньше {@code since}. */
    Optional<Notification> findLatestByDeduplicationKey(DeduplicationKey key, Instant since);

    /**
     * Освободить ключи идемпотентности старше {@code retention}: после этого повтор ключа
     * создаёт новое уведомление. Сами уведомления не удаляются.
     *
     * @return число освобождённых ключей
     */
    int releaseIdempotencyKeysOlderThan(Instant now, Duration retention);
}
