package kz.company.notification.domain.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Накопитель доменных событий агрегата. Используется композицией, а не наследованием:
 * агрегат держит экземпляр и отдаёт события через {@code pullDomainEvents()}.
 */
public final class AggregateEvents {

    private final List<DomainEvent> pending = new ArrayList<>();

    public void register(DomainEvent event) {
        pending.add(event);
    }

    /** Возвращает накопленные события и очищает список. */
    public List<DomainEvent> pull() {
        List<DomainEvent> events = List.copyOf(pending);
        pending.clear();
        return events;
    }
}
