package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.application.exception.ConcurrentIdempotentRequestException;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.DeduplicationKey;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import kz.company.notification.infrastructure.out.persistence.mapper.NotificationMapper;
import kz.company.notification.infrastructure.out.persistence.repository.NotificationJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Хранилище уведомлений на JPA. */
@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    static final String IDEMPOTENCY_CONSTRAINT = "uq_notification_client_idempotency_key";

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper mapper;

    public JpaNotificationRepositoryAdapter(NotificationJpaRepository jpaRepository, NotificationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(notification)));
        } catch (DataIntegrityViolationException violation) {
            String message = String.valueOf(violation.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains(IDEMPOTENCY_CONSTRAINT)) {
                throw new ConcurrentIdempotentRequestException();
            }
            throw violation;
        }
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(ClientId clientId, IdempotencyKey key) {
        return jpaRepository.findByClientIdAndIdempotencyKey(clientId.value(), key.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Notification> findLatestByDeduplicationKey(DeduplicationKey key, Instant since) {
        return jpaRepository.findFirstByDeduplicationKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(key.value(), since)
                .map(mapper::toDomain);
    }

    @Override
    public int releaseIdempotencyKeysOlderThan(Instant now, Duration retention) {
        return jpaRepository.releaseIdempotencyKeysCreatedBefore(now.minus(retention));
    }
}
