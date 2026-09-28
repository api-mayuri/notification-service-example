package kz.company.notification.application.service;

import kz.company.notification.application.port.in.DeliveryOutcome;
import kz.company.notification.application.port.in.ProcessDeliveryUseCase;
import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.application.settings.RateLimit;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.RateWindowUsage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.policy.RetryPolicy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Обработка доставки воркером канала в три шага:
 * <ol>
 *     <li>транзакция: проверить статус и лимит частоты, перевести в SENDING (попытка засчитана);</li>
 *     <li>без транзакции: вызвать провайдера канала — сетевой вызов не держит соединение с БД;</li>
 *     <li>транзакция: применить результат (SENT / RETRY_SCHEDULED / FAILED) и опубликовать события.</li>
 * </ol>
 * Если экземпляр упадёт между шагами 1 и 3, доставка останется в SENDING, и её вернёт в очередь
 * {@link DeliverySchedulingService#recover()}. Поэтому семантика — at-least-once.
 */
public class DeliveryProcessingService implements ProcessDeliveryUseCase {

    private final DeliveryRepository deliveryRepository;
    private final ChannelSenderRegistry channelRegistry;
    private final DomainEventPublisher eventPublisher;
    private final RetryPolicy retryPolicy;
    private final DeliverySettings settings;
    private final TransactionOperations transactions;
    private final Clock clock;

    public DeliveryProcessingService(DeliveryRepository deliveryRepository, ChannelSenderRegistry channelRegistry,
                                     DomainEventPublisher eventPublisher, RetryPolicy retryPolicy,
                                     DeliverySettings settings, TransactionOperations transactions, Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.channelRegistry = channelRegistry;
        this.eventPublisher = eventPublisher;
        this.retryPolicy = retryPolicy;
        this.settings = settings;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public DeliveryOutcome process(DeliveryId deliveryId) {
        StartResult start;
        try {
            start = transactions.execute(status -> startAttempt(deliveryId));
        } catch (OptimisticLockingFailureException concurrentUpdate) {
            return new DeliveryOutcome.Ignored("Delivery is being processed concurrently");
        }
        if (start == null || start.outcome() != null) {
            return start == null ? new DeliveryOutcome.Ignored("No result") : start.outcome();
        }
        SendResult result = send(start.delivery());
        return transactions.execute(status -> applyResult(deliveryId, result));
    }

    @Override
    public boolean park(DeliveryId deliveryId, String reason) {
        Boolean parked = transactions.execute(status -> {
            Optional<Delivery> found = deliveryRepository.findById(deliveryId);
            if (found.isEmpty()) {
                return false;
            }
            Delivery delivery = found.get();
            if (!delivery.park(reason, clock.instant())) {
                return false;
            }
            var events = delivery.pullDomainEvents();
            deliveryRepository.save(delivery);
            eventPublisher.publish(events);
            return true;
        });
        return Boolean.TRUE.equals(parked);
    }

    private StartResult startAttempt(DeliveryId deliveryId) {
        Instant now = clock.instant();
        Optional<Delivery> found = deliveryRepository.findById(deliveryId);
        if (found.isEmpty()) {
            return StartResult.finished(new DeliveryOutcome.Ignored("Delivery not found"));
        }
        Delivery delivery = found.get();
        if (!delivery.status().isAwaitingSend()) {
            return StartResult.finished(new DeliveryOutcome.Ignored("Delivery is " + delivery.status()));
        }
        Optional<Instant> deferUntil = rateLimitDeferral(delivery, now);
        if (deferUntil.isPresent()) {
            delivery.defer(deferUntil.get(), "rate limit for " + delivery.channel() + " reached", now);
            deliveryRepository.save(delivery);
            return StartResult.finished(new DeliveryOutcome.Deferred(deferUntil.get()));
        }
        delivery.startAttempt(now);
        return StartResult.proceed(deliveryRepository.save(delivery));
    }

    private Optional<Instant> rateLimitDeferral(Delivery delivery, Instant now) {
        Optional<RateLimit> limit = settings.rateLimitFor(delivery.channel())
                .filter(rateLimit -> rateLimit.appliesTo(delivery.category()));
        if (limit.isEmpty()) {
            return Optional.empty();
        }
        RateWindowUsage usage = deliveryRepository.rateWindowUsage(delivery.recipientId(), delivery.channel(),
                now.minus(limit.get().window()));
        if (usage.sentCount() < limit.get().maxDeliveries()) {
            return Optional.empty();
        }
        Instant windowFreesAt = usage.optionalOldestSentAt().orElse(now).plus(limit.get().window());
        return Optional.of(windowFreesAt.isAfter(now) ? windowFreesAt : now.plusSeconds(1));
    }

    private SendResult send(Delivery delivery) {
        Optional<ChannelSender> sender = channelRegistry.find(delivery.channel());
        if (sender.isEmpty()) {
            return new SendResult.PermanentFailure("Channel " + delivery.channel() + " is disabled");
        }
        try {
            return sender.get().send(delivery);
        } catch (RuntimeException unexpected) {
            return new SendResult.RetryableFailure(unexpected.getClass().getSimpleName() + ": " + unexpected.getMessage());
        }
    }

    private DeliveryOutcome applyResult(DeliveryId deliveryId, SendResult result) {
        Instant now = clock.instant();
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        if (delivery.status() != DeliveryStatus.SENDING) {
            // Доставку успели вернуть в очередь как «зависшую»: результат этой попытки теряется,
            // повтор отправит сообщение ещё раз (at-least-once).
            return new DeliveryOutcome.Ignored("Delivery moved to " + delivery.status() + " during sending");
        }
        delivery.recordResult(result, retryPolicy, now);
        var events = delivery.pullDomainEvents();
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publish(events);
        return switch (saved.status()) {
            case SENT -> new DeliveryOutcome.Sent(saved.providerMessageId().orElse(null), saved.simulated());
            case RETRY_SCHEDULED -> new DeliveryOutcome.RetryScheduled(saved.nextAttemptAt().orElse(now),
                    saved.lastError().orElse(""));
            default -> new DeliveryOutcome.Failed(saved.lastError().orElse(""),
                    result instanceof SendResult.PermanentFailure);
        };
    }

    private record StartResult(Delivery delivery, DeliveryOutcome outcome) {

        static StartResult proceed(Delivery delivery) {
            return new StartResult(delivery, null);
        }

        static StartResult finished(DeliveryOutcome outcome) {
            return new StartResult(null, outcome);
        }
    }
}
