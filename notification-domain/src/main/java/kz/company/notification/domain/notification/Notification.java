package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.AggregateEvents;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.notification.event.NotificationAccepted;
import kz.company.notification.domain.notification.event.NotificationCancelled;
import kz.company.notification.domain.notification.event.NotificationCompleted;
import kz.company.notification.domain.policy.NotificationStatusCalculator;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Агрегат «Уведомление»: принятый запрос на отправку. Хранит, кому, что и по каким каналам
 * нужно отправить; итоговый статус вычисляется из статусов доставок.
 */
public final class Notification {

    private final NotificationId id;
    private final NotificationRequest request;
    private final DeduplicationKey deduplicationKey;
    private final Instant createdAt;
    private final Long version;
    private final AggregateEvents events = new AggregateEvents();
    private NotificationStatus status;
    private Instant updatedAt;

    private Notification(NotificationId id, NotificationRequest request, DeduplicationKey deduplicationKey,
                         NotificationStatus status, Instant createdAt, Instant updatedAt, Long version) {
        this.id = Guard.notNull(id, "notificationId");
        this.request = Guard.notNull(request, "request");
        this.deduplicationKey = deduplicationKey;
        this.status = Guard.notNull(status, "status");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
        this.version = version;
    }

    /** Принять новое уведомление. Регистрирует событие {@link NotificationAccepted}. */
    public static Notification accept(NotificationId id, NotificationRequest request,
                                      DeduplicationKey deduplicationKey, Instant now) {
        Notification notification = new Notification(id, request, deduplicationKey,
                NotificationStatus.ACCEPTED, now, now, null);
        notification.events.register(new NotificationAccepted(
                id.value(), request.clientId().value(), request.recipientId().value(),
                request.category().name(), request.priority().name(),
                request.channels().stream().map(ChannelType::code).toList(), now));
        return notification;
    }

    /** Восстановить агрегат из хранилища. События не регистрируются. */
    public static Notification restore(NotificationSnapshot snapshot) {
        return new Notification(snapshot.id(), snapshot.request(), snapshot.deduplicationKey(),
                snapshot.status(), snapshot.createdAt(), snapshot.updatedAt(), snapshot.version());
    }

    public NotificationSnapshot snapshot() {
        return new NotificationSnapshot(id, request, deduplicationKey, status, createdAt, updatedAt, version);
    }

    /**
     * Пересчитать статус по статусам доставок. При переходе в завершённый статус регистрирует
     * {@link NotificationCompleted}. Отменённое уведомление не меняет статус.
     *
     * @return {@code true}, если статус изменился
     */
    public boolean refreshStatus(Collection<DeliveryStatus> deliveryStatuses, Instant now) {
        if (status == NotificationStatus.CANCELLED) {
            return false;
        }
        NotificationStatus calculated = NotificationStatusCalculator.calculate(deliveryStatuses);
        if (calculated == status) {
            return false;
        }
        status = calculated;
        updatedAt = now;
        if (calculated.isTerminal()) {
            events.register(new NotificationCompleted(id.value(), calculated.name(), now));
        }
        return true;
    }

    /**
     * Отменить уведомление. Возможно, пока ни одна доставка не началась (статус ACCEPTED).
     * Повторная отмена ничего не делает.
     *
     * @return {@code true}, если уведомление отменено этим вызовом
     * @throws NotificationNotCancellableException если отправка уже началась или завершилась
     */
    public boolean cancel(Instant now) {
        if (status == NotificationStatus.CANCELLED) {
            return false;
        }
        if (status != NotificationStatus.ACCEPTED) {
            throw new NotificationNotCancellableException(id, status);
        }
        status = NotificationStatus.CANCELLED;
        updatedAt = now;
        events.register(new NotificationCancelled(id.value(), now));
        return true;
    }

    /** Совпадает ли запрос с исходным (для идемпотентного повтора). */
    public boolean hasSameRequestAs(String requestFingerprint) {
        return request.requestFingerprint() != null && request.requestFingerprint().equals(requestFingerprint);
    }

    public List<DomainEvent> pullDomainEvents() {
        return events.pull();
    }

    public NotificationId id() {
        return id;
    }

    public NotificationRequest request() {
        return request;
    }

    public ClientId clientId() {
        return request.clientId();
    }

    public RecipientId recipientId() {
        return request.recipientId();
    }

    public Category category() {
        return request.category();
    }

    public Priority priority() {
        return request.priority();
    }

    public Set<ChannelType> requestedChannels() {
        return request.channels();
    }

    public Map<String, String> metadata() {
        return request.metadata();
    }

    public Optional<DeduplicationKey> deduplicationKey() {
        return Optional.ofNullable(deduplicationKey);
    }

    public NotificationStatus status() {
        return status;
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
