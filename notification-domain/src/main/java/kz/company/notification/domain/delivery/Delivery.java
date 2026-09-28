package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.AggregateEvents;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.event.DeliveryCancelled;
import kz.company.notification.domain.delivery.event.DeliveryConfirmed;
import kz.company.notification.domain.delivery.event.DeliveryFailed;
import kz.company.notification.domain.delivery.event.DeliveryRetryScheduled;
import kz.company.notification.domain.delivery.event.DeliveryScheduled;
import kz.company.notification.domain.delivery.event.DeliverySent;
import kz.company.notification.domain.delivery.event.DeliverySkipped;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.policy.RetryDecision;
import kz.company.notification.domain.policy.RetryPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Агрегат «Доставка»: отправка одного уведомления по одному каналу. Отдельный агрегат,
 * потому что доставки по разным каналам обновляются параллельно и независимо.
 *
 * <p>Жизненный цикл — явная машина состояний ({@link DeliveryStatus}); недопустимый
 * переход бросает {@link InvalidDeliveryStatusTransitionException}.
 */
public final class Delivery {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final DeliveryId id;
    private final NotificationId notificationId;
    private final RecipientId recipientId;
    private final ChannelType channel;
    private final ChannelAddress address;
    private final RenderedMessage message;
    private final Category category;
    private final Priority priority;
    private final Instant createdAt;
    private final Long version;
    private final AggregateEvents events = new AggregateEvents();

    private DeliveryStatus status;
    private int attempts;
    private Instant nextAttemptAt;
    private Instant dispatchedAt;
    private String lastError;
    private String providerMessageId;
    private boolean simulated;
    private SkipReason skipReason;
    private Instant sentAt;
    private Instant updatedAt;

    private Delivery(DeliverySnapshot snapshot) {
        this.id = Guard.notNull(snapshot.id(), "deliveryId");
        this.notificationId = Guard.notNull(snapshot.notificationId(), "notificationId");
        this.recipientId = Guard.notNull(snapshot.recipientId(), "recipientId");
        this.channel = Guard.notNull(snapshot.channel(), "channel");
        this.address = snapshot.address();
        this.message = snapshot.message();
        this.category = Guard.notNull(snapshot.category(), "category");
        this.priority = Guard.notNull(snapshot.priority(), "priority");
        this.status = Guard.notNull(snapshot.status(), "status");
        this.attempts = snapshot.attempts();
        this.nextAttemptAt = snapshot.nextAttemptAt();
        this.dispatchedAt = snapshot.dispatchedAt();
        this.lastError = snapshot.lastError();
        this.providerMessageId = snapshot.providerMessageId();
        this.simulated = snapshot.simulated();
        this.skipReason = snapshot.skipReason();
        this.sentAt = snapshot.sentAt();
        this.createdAt = Guard.notNull(snapshot.createdAt(), "createdAt");
        this.updatedAt = Guard.notNull(snapshot.updatedAt(), "updatedAt");
        this.version = snapshot.version();
        if (status != DeliveryStatus.SKIPPED && (address == null || message == null)) {
            throw new IllegalArgumentException("Only skipped deliveries may have no address or message");
        }
    }

    /** Новая доставка в статусе PENDING, которая будет отправлена не раньше {@code scheduledFor}. */
    static Delivery schedule(DeliveryId id, NotificationId notificationId, RecipientId recipientId,
                             ChannelType channel, ChannelAddress address, RenderedMessage message,
                             Category category, Priority priority, Instant scheduledFor, Instant now) {
        Guard.notNull(address, "address");
        Guard.notNull(message, "message");
        Delivery delivery = new Delivery(new DeliverySnapshot(id, notificationId, recipientId, channel, address, message,
                category, priority, DeliveryStatus.PENDING, 0, scheduledFor, null, null, null, false, null,
                null, now, now, null));
        delivery.events.register(new DeliveryScheduled(id.value(), notificationId.value(), recipientId.value(),
                channel.code(), priority.name(), scheduledFor, now));
        return delivery;
    }

    /** Доставка, которая сразу пропускается (отписка, нет контакта). */
    static Delivery skipped(DeliveryId id, NotificationId notificationId, RecipientId recipientId,
                            ChannelType channel, Category category, Priority priority, SkipReason reason, Instant now) {
        Delivery delivery = new Delivery(new DeliverySnapshot(id, notificationId, recipientId, channel, null, null,
                category, priority, DeliveryStatus.SKIPPED, 0, null, null, null, null, false, reason,
                null, now, now, null));
        delivery.events.register(new DeliverySkipped(id.value(), notificationId.value(), channel.code(),
                reason.name(), now));
        return delivery;
    }

    /** Восстановить агрегат из хранилища. События не регистрируются. */
    public static Delivery restore(DeliverySnapshot snapshot) {
        return new Delivery(snapshot);
    }

    public DeliverySnapshot snapshot() {
        return new DeliverySnapshot(id, notificationId, recipientId, channel, address, message, category, priority,
                status, attempts, nextAttemptAt, dispatchedAt, lastError, providerMessageId, simulated, skipReason,
                sentAt, createdAt, updatedAt, version);
    }

    /** Пора ли отправлять (для планировщика). */
    public boolean isDue(Instant now) {
        return status.isAwaitingSend() && dispatchedAt == null && nextAttemptAt != null && !nextAttemptAt.isAfter(now);
    }

    /** Отметить, что доставка поставлена в топик канала. */
    public void markDispatched(Instant now) {
        requireAwaitingSend("dispatch");
        dispatchedAt = now;
        updatedAt = now;
    }

    /** Начать попытку отправки: PENDING | RETRY_SCHEDULED → SENDING. */
    public void startAttempt(Instant now) {
        transitionTo(DeliveryStatus.SENDING, now);
        attempts++;
    }

    /**
     * Применить результат попытки: успех → SENT; временная ошибка → RETRY_SCHEDULED или FAILED
     * (если попытки исчерпаны); постоянная ошибка → FAILED.
     */
    public void recordResult(SendResult result, RetryPolicy retryPolicy, Instant now) {
        Guard.notNull(result, "result");
        switch (result) {
            case SendResult.Success success -> markSent(success, now);
            case SendResult.RetryableFailure failure -> scheduleRetryOrFail(failure.reason(), retryPolicy, now);
            case SendResult.PermanentFailure failure -> fail(failure.reason(), true, now);
        }
    }

    /**
     * Попытка «зависла» в SENDING (экземпляр упал во время отправки). Доставка возвращается
     * в очередь или завершается ошибкой, если попытки исчерпаны.
     */
    public void recoverStaleAttempt(RetryPolicy retryPolicy, Instant now) {
        if (status != DeliveryStatus.SENDING) {
            throw new InvalidDeliveryStatusTransitionException(id, status, DeliveryStatus.RETRY_SCHEDULED);
        }
        if (retryPolicy.hasAttemptsLeft(attempts)) {
            transitionTo(DeliveryStatus.RETRY_SCHEDULED, now);
            nextAttemptAt = now;
            dispatchedAt = null;
            lastError = truncate("Attempt interrupted: sending state became stale");
            events.register(new DeliveryRetryScheduled(id.value(), notificationId.value(), channel.code(), attempts,
                    now, lastError, now));
        } else {
            fail("Attempt interrupted and no attempts left", false, now);
        }
    }

    /** Вернуть в очередь доставку, чьё сообщение о повторе потерялось. Статус не меняется. */
    public void requeue(Instant now) {
        requireAwaitingSend("requeue");
        dispatchedAt = null;
        nextAttemptAt = now;
        updatedAt = now;
    }

    /** Отложить отправку (лимит частоты). Статус не меняется, попытка не расходуется. */
    public void defer(Instant until, String reason, Instant now) {
        requireAwaitingSend("defer");
        nextAttemptAt = Guard.notNull(until, "until");
        dispatchedAt = null;
        lastError = truncate("Deferred: " + reason);
        updatedAt = now;
    }

    /** Отменить, если отправка ещё не начиналась. @return {@code true}, если отменено этим вызовом */
    public boolean cancel(Instant now) {
        if (!status.isAwaitingSend()) {
            return false;
        }
        transitionTo(DeliveryStatus.CANCELLED, now);
        events.register(new DeliveryCancelled(id.value(), notificationId.value(), channel.code(), now));
        return true;
    }

    /** Перевести в FAILED без повторов (сообщение ушло в parking lot). Завершённые доставки не меняются. */
    public boolean park(String reason, Instant now) {
        if (status.isTerminal()) {
            return false;
        }
        fail(reason, true, now);
        return true;
    }

    /** Провайдер подтвердил доставку: SENT → DELIVERED. */
    public void confirmDelivered(Instant now) {
        transitionTo(DeliveryStatus.DELIVERED, now);
        events.register(new DeliveryConfirmed(id.value(), notificationId.value(), channel.code(), now));
    }

    public List<DomainEvent> pullDomainEvents() {
        return events.pull();
    }

    private void markSent(SendResult.Success success, Instant now) {
        transitionTo(DeliveryStatus.SENT, now);
        providerMessageId = success.providerMessageId();
        simulated = success.simulated();
        sentAt = now;
        lastError = null;
        events.register(new DeliverySent(id.value(), notificationId.value(), channel.code(), attempts,
                providerMessageId, simulated, now));
    }

    private void scheduleRetryOrFail(String reason, RetryPolicy retryPolicy, Instant now) {
        RetryDecision decision = retryPolicy.decide(attempts, now);
        switch (decision) {
            case RetryDecision.RetryAt retry -> {
                transitionTo(DeliveryStatus.RETRY_SCHEDULED, now);
                nextAttemptAt = retry.at();
                lastError = truncate(reason);
                events.register(new DeliveryRetryScheduled(id.value(), notificationId.value(), channel.code(), attempts,
                        nextAttemptAt, lastError, now));
            }
            case RetryDecision.GiveUp giveUp -> fail("Attempts exhausted (" + attempts + "): " + reason, false, now);
        }
    }

    private void fail(String reason, boolean permanent, Instant now) {
        transitionTo(DeliveryStatus.FAILED, now);
        lastError = truncate(reason);
        nextAttemptAt = null;
        events.register(new DeliveryFailed(id.value(), notificationId.value(), channel.code(), attempts, lastError,
                permanent, now));
    }

    private void transitionTo(DeliveryStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidDeliveryStatusTransitionException(id, status, target);
        }
        status = target;
        updatedAt = Guard.notNull(now, "now");
    }

    private void requireAwaitingSend(String operation) {
        if (!status.isAwaitingSend()) {
            throw new InvalidDeliveryStatusTransitionException(id, status, operation);
        }
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    public DeliveryId id() {
        return id;
    }

    public NotificationId notificationId() {
        return notificationId;
    }

    public RecipientId recipientId() {
        return recipientId;
    }

    public ChannelType channel() {
        return channel;
    }

    public Optional<ChannelAddress> address() {
        return Optional.ofNullable(address);
    }

    public Optional<RenderedMessage> message() {
        return Optional.ofNullable(message);
    }

    public Category category() {
        return category;
    }

    public Priority priority() {
        return priority;
    }

    public DeliveryStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Optional<Instant> nextAttemptAt() {
        return Optional.ofNullable(nextAttemptAt);
    }

    public Optional<Instant> dispatchedAt() {
        return Optional.ofNullable(dispatchedAt);
    }

    public Optional<String> lastError() {
        return Optional.ofNullable(lastError);
    }

    public Optional<String> providerMessageId() {
        return Optional.ofNullable(providerMessageId);
    }

    public boolean simulated() {
        return simulated;
    }

    public Optional<SkipReason> skipReason() {
        return Optional.ofNullable(skipReason);
    }

    public Optional<Instant> sentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
    }
}
