package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.domain.common.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public final class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(List<? extends DomainEvent> published) {
        events.addAll(published);
    }

    public List<DomainEvent> events() {
        return events;
    }

    public <T extends DomainEvent> List<T> eventsOf(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public void clear() {
        events.clear();
    }
}
