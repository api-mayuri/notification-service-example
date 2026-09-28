package kz.company.notification.application.fake;

import org.springframework.dao.OptimisticLockingFailureException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Хранилище снимков с проверкой версии, как у JPA с {@code @Version}. */
final class VersionedStore<K, S> {

    private final Map<K, S> snapshots = new LinkedHashMap<>();
    private final Map<K, Long> versions = new LinkedHashMap<>();

    /** @return новая версия */
    long put(K key, Long expectedVersion, Function<Long, S> snapshotWithVersion) {
        Long current = versions.get(key);
        if (current != null && !current.equals(expectedVersion)) {
            throw new OptimisticLockingFailureException("Stale version for " + key);
        }
        if (current == null && expectedVersion != null) {
            throw new OptimisticLockingFailureException("Entity " + key + " was deleted");
        }
        long next = current == null ? 0 : current + 1;
        versions.put(key, next);
        snapshots.put(key, snapshotWithVersion.apply(next));
        return next;
    }

    Optional<S> get(K key) {
        return Optional.ofNullable(snapshots.get(key));
    }

    Iterable<S> values() {
        return snapshots.values();
    }
}
