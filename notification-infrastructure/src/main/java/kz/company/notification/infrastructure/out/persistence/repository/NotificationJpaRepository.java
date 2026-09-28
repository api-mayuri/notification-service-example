package kz.company.notification.infrastructure.out.persistence.repository;

import kz.company.notification.infrastructure.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID>,
        JpaSpecificationExecutor<NotificationJpaEntity> {

    Optional<NotificationJpaEntity> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);

    Optional<NotificationJpaEntity> findFirstByDeduplicationKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String deduplicationKey, Instant since);

    @Modifying
    @Query("update NotificationJpaEntity n set n.idempotencyKey = null "
            + "where n.idempotencyKey is not null and n.createdAt < :before")
    int releaseIdempotencyKeysCreatedBefore(@Param("before") Instant before);
}
