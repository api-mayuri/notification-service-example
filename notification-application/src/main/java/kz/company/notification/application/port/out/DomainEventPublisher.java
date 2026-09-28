package kz.company.notification.application.port.out;

import kz.company.notification.domain.common.DomainEvent;

import java.util.List;

/**
 * Публикация доменных событий. Вызывается внутри транзакции сценария: реализация сохраняет
 * события в журнал публикаций (transactional outbox) в той же транзакции, а доставляет
 * подписчикам и в брокер — после коммита.
 */
public interface DomainEventPublisher {

    void publish(List<? extends DomainEvent> events);
}
