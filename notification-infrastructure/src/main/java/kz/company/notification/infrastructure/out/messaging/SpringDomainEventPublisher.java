package kz.company.notification.infrastructure.out.messaging;

import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.domain.common.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Публикация доменных событий через {@link ApplicationEventPublisher} внутри транзакции сценария.
 * Spring Modulith Event Publication Registry сохраняет публикации в той же транзакции (outbox):
 * <ul>
 *     <li>само доменное событие — для внутренних слушателей ({@code @ApplicationModuleListener});</li>
 *     <li>его внешнее представление ({@code ExternalMessage}) — для externalization в Kafka.</li>
 * </ul>
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final StatusEventMapper statusEventMapper;

    public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher, StatusEventMapper statusEventMapper) {
        this.eventPublisher = eventPublisher;
        this.statusEventMapper = statusEventMapper;
    }

    @Override
    public void publish(List<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            eventPublisher.publishEvent(event);
            statusEventMapper.toExternal(event).ifPresent(eventPublisher::publishEvent);
        }
    }
}
