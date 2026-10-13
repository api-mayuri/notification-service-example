package kz.company.notification.application.fake;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.delivery.DeliverySnapshot;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.RateWindowUsage;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.RecipientId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class InMemoryDeliveryRepository implements DeliveryRepository {

    private final VersionedStore<DeliveryId, DeliverySnapshot> store = new VersionedStore<>();

    @Override
    public Delivery save(Delivery delivery) {
        DeliverySnapshot s = delivery.snapshot();
        store.put(s.id(), s.version(), version -> new DeliverySnapshot(s.id(), s.notificationId(), s.recipientId(),
                s.channel(), s.address(), s.message(), s.category(), s.priority(), s.status(), s.attempts(),
                s.nextAttemptAt(), s.dispatchedAt(), s.lastError(), s.providerMessageId(), s.simulated(),
                s.skipReason(), s.sentAt(), s.createdAt(), s.updatedAt(), version));
        return findById(s.id()).orElseThrow();
    }

    @Override
    public List<Delivery> saveAll(List<Delivery> deliveries) {
        return deliveries.stream().map(this::save).toList();
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        return store.get(id).map(Delivery::restore);
    }

    @Override
    public List<Delivery> findByNotificationId(NotificationId notificationId) {
        return select(delivery -> delivery.notificationId().equals(notificationId), Integer.MAX_VALUE);
    }

    @Override
    public List<Delivery> lockDueForDispatch(Instant now, int limit) {
        return select(delivery -> delivery.isDue(now), limit);
    }

    @Override
    public List<Delivery> lockStaleSending(Instant updatedBefore, int limit) {
        return select(delivery -> delivery.status() == DeliveryStatus.SENDING
                && delivery.updatedAt().isBefore(updatedBefore), limit);
    }

    @Override
    public List<Delivery> lockOverdueDispatches(Instant dueBefore, int limit) {
        return select(delivery -> delivery.status().isAwaitingSend()
                && delivery.dispatchedAt().filter(dispatched -> dispatched.isBefore(dueBefore)).isPresent()
                && delivery.nextAttemptAt().filter(next -> next.isBefore(dueBefore)).isPresent(), limit);
    }

    @Override
    public RateWindowUsage rateWindowUsage(RecipientId recipientId, ChannelType channel, Instant since) {
        List<Instant> sent = all().stream()
                .filter(delivery -> delivery.recipientId().equals(recipientId) && delivery.channel().equals(channel))
                .flatMap(delivery -> delivery.sentAt().stream())
                .filter(sentAt -> !sentAt.isBefore(since))
                .sorted()
                .toList();
        return sent.isEmpty() ? RateWindowUsage.EMPTY : new RateWindowUsage(sent.size(), sent.getFirst());
    }

    public List<Delivery> all() {
        List<Delivery> result = new ArrayList<>();
        store.values().forEach(snapshot -> result.add(Delivery.restore(snapshot)));
        return result;
    }

    private List<Delivery> select(Predicate<Delivery> filter, int limit) {
        return all().stream().filter(filter).sorted(Comparator.comparing(Delivery::createdAt)).limit(limit).toList();
    }
}
