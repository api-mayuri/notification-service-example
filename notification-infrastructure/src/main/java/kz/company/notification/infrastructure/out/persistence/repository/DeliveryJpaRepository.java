package kz.company.notification.infrastructure.out.persistence.repository;

import kz.company.notification.infrastructure.out.persistence.entity.DeliveryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Доставки. Запросы планировщика блокируют строки {@code FOR UPDATE SKIP LOCKED}: несколько
 * экземпляров сервиса разбирают разные доставки и не ждут друг друга.
 */
public interface DeliveryJpaRepository extends JpaRepository<DeliveryJpaEntity, UUID> {

    List<DeliveryJpaEntity> findByNotificationIdOrderByChannelAsc(UUID notificationId);

    @Query(value = """
            select * from delivery
            where status in ('PENDING', 'RETRY_SCHEDULED')
              and dispatched_at is null
              and next_attempt_at <= :now
            order by next_attempt_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<DeliveryJpaEntity> lockDueForDispatch(@Param("now") Instant now, @Param("limit") int limit);

    @Query(value = """
            select * from delivery
            where status = 'SENDING'
              and updated_at < :updatedBefore
            order by updated_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<DeliveryJpaEntity> lockStaleSending(@Param("updatedBefore") Instant updatedBefore, @Param("limit") int limit);

    @Query(value = """
            select * from delivery
            where status = 'RETRY_SCHEDULED'
              and dispatched_at is not null
              and next_attempt_at < :dueBefore
            order by next_attempt_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<DeliveryJpaEntity> lockOverdueRetries(@Param("dueBefore") Instant dueBefore, @Param("limit") int limit);

    @Query("""
            select count(d), min(d.sentAt) from DeliveryJpaEntity d
            where d.recipientId = :recipientId and d.channel = :channel and d.sentAt >= :since
            """)
    List<Object[]> rateWindowUsage(@Param("recipientId") String recipientId, @Param("channel") String channel,
                                   @Param("since") Instant since);
}
