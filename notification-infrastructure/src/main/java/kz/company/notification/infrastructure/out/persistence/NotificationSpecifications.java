package kz.company.notification.infrastructure.out.persistence;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import kz.company.notification.application.port.in.NotificationSearchCriteria;
import kz.company.notification.infrastructure.out.persistence.entity.DeliveryJpaEntity;
import kz.company.notification.infrastructure.out.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Фильтры поиска уведомлений (паттерн Specification). Каждый фильтр — отдельная спецификация. */
final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    static Specification<NotificationJpaEntity> matching(NotificationSearchCriteria criteria) {
        List<Specification<NotificationJpaEntity>> filters = new ArrayList<>();
        filters.add(ofClient(criteria.clientId().value()));
        if (criteria.recipientId() != null) {
            filters.add(ofRecipient(criteria.recipientId().value()));
        }
        if (criteria.status() != null) {
            filters.add(withStatus(criteria.status().name()));
        }
        if (criteria.channel() != null) {
            filters.add(withDeliveryChannel(criteria.channel().code()));
        }
        if (criteria.from() != null) {
            filters.add(createdFrom(criteria.from()));
        }
        if (criteria.to() != null) {
            filters.add(createdBefore(criteria.to()));
        }
        Specification<NotificationJpaEntity> combined = filters.getFirst();
        for (Specification<NotificationJpaEntity> filter : filters.subList(1, filters.size())) {
            combined = combined.and(filter);
        }
        return combined;
    }

    static Specification<NotificationJpaEntity> ofClient(String clientId) {
        return (root, query, builder) -> builder.equal(root.get("clientId"), clientId);
    }

    static Specification<NotificationJpaEntity> ofRecipient(String recipientId) {
        return (root, query, builder) -> builder.equal(root.get("recipientId"), recipientId);
    }

    static Specification<NotificationJpaEntity> withStatus(String status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    static Specification<NotificationJpaEntity> createdFrom(Instant from) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<NotificationJpaEntity> createdBefore(Instant to) {
        return (root, query, builder) -> builder.lessThan(root.get("createdAt"), to);
    }

    /** Есть доставка по каналу. */
    static Specification<NotificationJpaEntity> withDeliveryChannel(String channel) {
        return (root, query, builder) -> {
            Subquery<UUID> deliveries = query.subquery(UUID.class);
            Root<DeliveryJpaEntity> delivery = deliveries.from(DeliveryJpaEntity.class);
            deliveries.select(delivery.get("id")).where(
                    builder.equal(delivery.get("notificationId"), root.get("id")),
                    builder.equal(delivery.get("channel"), channel));
            return builder.exists(deliveries);
        };
    }
}
