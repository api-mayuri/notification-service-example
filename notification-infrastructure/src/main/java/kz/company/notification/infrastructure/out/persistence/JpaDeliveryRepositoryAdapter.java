package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.delivery.RateWindowUsage;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.infrastructure.out.persistence.entity.DeliveryJpaEntity;
import kz.company.notification.infrastructure.out.persistence.mapper.DeliveryMapper;
import kz.company.notification.infrastructure.out.persistence.repository.DeliveryJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Хранилище доставок на JPA. */
@Repository
public class JpaDeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository jpaRepository;
    private final DeliveryMapper mapper;

    public JpaDeliveryRepositoryAdapter(DeliveryJpaRepository jpaRepository, DeliveryMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Delivery save(Delivery delivery) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(delivery)));
    }

    @Override
    public List<Delivery> saveAll(List<Delivery> deliveries) {
        List<DeliveryJpaEntity> saved = jpaRepository.saveAll(deliveries.stream().map(mapper::toEntity).toList());
        jpaRepository.flush();
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Delivery> findByNotificationId(NotificationId notificationId) {
        return jpaRepository.findByNotificationIdOrderByChannelAsc(notificationId.value()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Delivery> lockDueForDispatch(Instant now, int limit) {
        return jpaRepository.lockDueForDispatch(now, limit).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Delivery> lockStaleSending(Instant updatedBefore, int limit) {
        return jpaRepository.lockStaleSending(updatedBefore, limit).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Delivery> lockOverdueRetries(Instant dueBefore, int limit) {
        return jpaRepository.lockOverdueRetries(dueBefore, limit).stream().map(mapper::toDomain).toList();
    }

    @Override
    public RateWindowUsage rateWindowUsage(RecipientId recipientId, ChannelType channel, Instant since) {
        List<Object[]> rows = jpaRepository.rateWindowUsage(recipientId.value(), channel.code(), since);
        if (rows.isEmpty() || rows.getFirst()[0] == null) {
            return RateWindowUsage.EMPTY;
        }
        Object[] row = rows.getFirst();
        return new RateWindowUsage(((Number) row[0]).longValue(), (Instant) row[1]);
    }
}
