package kz.company.notification.application.fake;

import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.DeduplicationKey;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.NotificationSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryNotificationRepository implements NotificationRepository {

    private final VersionedStore<NotificationId, NotificationSnapshot> store = new VersionedStore<>();

    @Override
    public Notification save(Notification notification) {
        NotificationSnapshot snapshot = notification.snapshot();
        store.put(snapshot.id(), snapshot.version(), version -> new NotificationSnapshot(snapshot.id(),
                snapshot.request(), snapshot.deduplicationKey(), snapshot.status(), snapshot.createdAt(),
                snapshot.updatedAt(), version));
        return findById(snapshot.id()).orElseThrow();
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return store.get(id).map(Notification::restore);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(ClientId clientId, IdempotencyKey key) {
        return all().stream()
                .filter(notification -> notification.clientId().equals(clientId))
                .filter(notification -> Objects.equals(notification.request().idempotencyKey(), key))
                .findFirst();
    }

    @Override
    public Optional<Notification> findLatestByDeduplicationKey(DeduplicationKey key, Instant since) {
        return all().stream()
                .filter(notification -> notification.deduplicationKey().filter(key::equals).isPresent())
                .filter(notification -> !notification.createdAt().isBefore(since))
                .max(Comparator.comparing(Notification::createdAt));
    }

    @Override
    public int releaseIdempotencyKeysOlderThan(Instant now, Duration retention) {
        int released = 0;
        for (Notification notification : all()) {
            if (notification.request().idempotencyKey() != null
                    && notification.createdAt().isBefore(now.minus(retention))) {
                NotificationSnapshot snapshot = notification.snapshot();
                NotificationRequest request = snapshot.request();
                NotificationRequest withoutKey = new NotificationRequest(request.clientId(), null,
                        request.requestFingerprint(), request.recipient(), request.content(), request.channels(),
                        request.category(), request.priority(), request.sendAt(), request.metadata());
                save(Notification.restore(new NotificationSnapshot(snapshot.id(), withoutKey,
                        snapshot.deduplicationKey(), snapshot.status(), snapshot.createdAt(), snapshot.updatedAt(),
                        snapshot.version())));
                released++;
            }
        }
        return released;
    }

    public List<Notification> all() {
        List<Notification> result = new ArrayList<>();
        store.values().forEach(snapshot -> result.add(Notification.restore(snapshot)));
        return result;
    }
}
