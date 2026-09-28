package kz.company.notification.infrastructure.in.scheduling;

import kz.company.notification.application.port.in.DispatchDueDeliveriesUseCase;
import kz.company.notification.application.port.in.RecoverStuckDeliveriesUseCase;
import kz.company.notification.application.port.in.ReleaseExpiredIdempotencyKeysUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Входящий адаптер «время»: периодические задачи. Защита от параллельной работы нескольких
 * экземпляров — в самих сценариях ({@code FOR UPDATE SKIP LOCKED}), поэтому задачи безопасно
 * запускать на каждом экземпляре.
 */
@Component
@ConditionalOnProperty(prefix = "notification.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduledJobs.class);

    private final DispatchDueDeliveriesUseCase dispatchDueDeliveries;
    private final RecoverStuckDeliveriesUseCase recoverStuckDeliveries;
    private final ReleaseExpiredIdempotencyKeysUseCase releaseExpiredIdempotencyKeys;
    private final IncompleteEventPublications incompleteEventPublications;
    private final SchedulingProperties properties;

    public NotificationScheduledJobs(DispatchDueDeliveriesUseCase dispatchDueDeliveries,
                                     RecoverStuckDeliveriesUseCase recoverStuckDeliveries,
                                     ReleaseExpiredIdempotencyKeysUseCase releaseExpiredIdempotencyKeys,
                                     IncompleteEventPublications incompleteEventPublications,
                                     SchedulingProperties properties) {
        this.dispatchDueDeliveries = dispatchDueDeliveries;
        this.recoverStuckDeliveries = recoverStuckDeliveries;
        this.releaseExpiredIdempotencyKeys = releaseExpiredIdempotencyKeys;
        this.incompleteEventPublications = incompleteEventPublications;
        this.properties = properties;
    }

    /** Отложенные доставки (sendAt, тихие часы, лимит частоты), у которых подошло время. */
    @Scheduled(fixedDelayString = "${notification.scheduler.dispatch-interval:5s}")
    public void dispatchDueDeliveries() {
        int dispatched;
        do {
            dispatched = dispatchDueDeliveries.dispatchDue();
            if (dispatched > 0) {
                log.info("Dispatched {} due deliveries", dispatched);
            }
        } while (dispatched > 0);
    }

    /** Зависшие в SENDING и просроченные повторы. */
    @Scheduled(fixedDelayString = "${notification.scheduler.recovery-interval:1m}")
    public void recoverStuckDeliveries() {
        int recovered = recoverStuckDeliveries.recover();
        if (recovered > 0) {
            log.warn("Recovered {} stuck deliveries", recovered);
        }
    }

    /** Ключи идемпотентности старше срока хранения. */
    @Scheduled(fixedDelayString = "${notification.scheduler.idempotency-cleanup-interval:1h}")
    public void releaseExpiredIdempotencyKeys() {
        int released = releaseExpiredIdempotencyKeys.releaseExpired();
        if (released > 0) {
            log.info("Released {} expired idempotency keys", released);
        }
    }

    /** Повторная отправка незавершённых публикаций outbox (Kafka была недоступна, экземпляр упал). */
    @Scheduled(fixedDelayString = "${notification.scheduler.outbox-resubmit-interval:1m}")
    public void resubmitIncompletePublications() {
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(properties.outboxResubmitAfter());
    }
}
