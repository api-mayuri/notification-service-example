package kz.company.notification.application.service;

import kz.company.notification.application.port.in.DispatchDueDeliveriesUseCase;
import kz.company.notification.application.port.in.RecoverStuckDeliveriesUseCase;
import kz.company.notification.application.port.out.DeliveryDispatcher;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.policy.RetryPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Планировщик доставок: ставит в очередь доставки, у которых подошло время, и возвращает
 * в очередь зависшие. Строки блокируются {@code FOR UPDATE SKIP LOCKED}, поэтому несколько
 * экземпляров не обработают одну доставку дважды.
 */
public class DeliverySchedulingService implements DispatchDueDeliveriesUseCase, RecoverStuckDeliveriesUseCase {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDispatcher deliveryDispatcher;
    private final DomainEventPublisher eventPublisher;
    private final RetryPolicy retryPolicy;
    private final DeliverySettings settings;
    private final Clock clock;

    public DeliverySchedulingService(DeliveryRepository deliveryRepository, DeliveryDispatcher deliveryDispatcher,
                                     DomainEventPublisher eventPublisher, RetryPolicy retryPolicy,
                                     DeliverySettings settings, Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryDispatcher = deliveryDispatcher;
        this.eventPublisher = eventPublisher;
        this.retryPolicy = retryPolicy;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int dispatchDue() {
        Instant now = clock.instant();
        List<Delivery> due = deliveryRepository.lockDueForDispatch(now, settings.batchSize());
        for (Delivery delivery : due) {
            delivery.markDispatched(now);
            deliveryDispatcher.dispatch(deliveryRepository.save(delivery));
        }
        return due.size();
    }

    @Override
    @Transactional
    public int recover() {
        Instant now = clock.instant();
        List<DomainEvent> events = new ArrayList<>();
        List<Delivery> stale = deliveryRepository.lockStaleSending(now.minus(settings.staleSendingThreshold()),
                settings.batchSize());
        for (Delivery delivery : stale) {
            delivery.recoverStaleAttempt(retryPolicy, now);
            events.addAll(delivery.pullDomainEvents());
            deliveryRepository.save(delivery);
        }
        List<Delivery> overdue = deliveryRepository.lockOverdueRetries(now.minus(settings.overdueRetryGrace()),
                settings.batchSize());
        for (Delivery delivery : overdue) {
            delivery.requeue(now);
            deliveryRepository.save(delivery);
        }
        eventPublisher.publish(events);
        return stale.size() + overdue.size();
    }
}
