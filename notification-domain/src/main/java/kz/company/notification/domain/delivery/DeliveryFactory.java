package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.notification.Notification;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Фабрика доставок: создаёт по одной {@link Delivery} на каждый канал уведомления по его плану.
 */
public final class DeliveryFactory {

    private final Supplier<DeliveryId> idSupplier;

    public DeliveryFactory(Supplier<DeliveryId> idSupplier) {
        this.idSupplier = Guard.notNull(idSupplier, "idSupplier");
    }

    public List<Delivery> create(Notification notification, List<DeliveryPlan> plans, Instant now) {
        Guard.notEmpty(plans, "plans");
        return plans.stream().map(plan -> create(notification, plan, now)).toList();
    }

    private Delivery create(Notification notification, DeliveryPlan plan, Instant now) {
        return switch (plan) {
            case DeliveryPlan.Deliverable deliverable -> Delivery.schedule(idSupplier.get(), notification.id(),
                    notification.recipientId(), deliverable.channel(), deliverable.address(), deliverable.message(),
                    notification.category(), notification.priority(), deliverable.scheduledFor(), now);
            case DeliveryPlan.Skip skip -> Delivery.skipped(idSupplier.get(), notification.id(),
                    notification.recipientId(), skip.channel(), notification.category(), notification.priority(),
                    skip.reason(), now);
        };
    }
}
