# ADR 0004. Transactional outbox на Spring Modulith; один путь в Kafka

- **Статус:** принято

## Контекст

Промпт требует готовый outbox (Spring Modulith Event Publication Registry + externalization в Kafka). Возникли два противоречия:

1. **`@Externalized` и чистый домен.** В домене не может быть аннотаций фреймворка, а `@Externalized` — это аннотация Spring Modulith.
2. **Порт `DeliveryDispatcher → Kafka`.** Прямая отправка в Kafka в обход outbox ломает атомарность «БД + сообщение».

## Решение

- Доменные события остаются чистыми records. Выбор, маршрутизация и заголовки задаются программно в `EventExternalizationConfiguration` (bootstrap, `KafkaConfiguration`).
- Наружу уходит только инфраструктурный тип `ExternalMessage` (topic, key, CloudEvents-атрибуты, JSON-payload). Он публикуется как событие приложения внутри транзакции сценария, Modulith сохраняет его в `event_publication` и после коммита отправляет в Kafka.
- `DeliveryDispatcher` реализован через этот же механизм (`KafkaDeliveryDispatcher`). Путь в Kafka один, и он всегда идёт через outbox.
- `SpringDomainEventPublisher` публикует каждое доменное событие дважды:
  - как есть — для внутренних слушателей (`@ApplicationModuleListener` пересчитывает статус уведомления);
  - в виде `ExternalMessage` — для `notification.events.v1`.
- Сериализация значения — `ExternalMessageSerializer` (payload как есть), заголовки CloudEvents — через `headers(ExternalMessage.class, …)`.
- Таблица `event_publication` создаётся Liquibase, `spring.modulith.events.jdbc.schema-initialization.enabled=false`.
- `completion-mode: delete` — завершённые публикации удаляются, таблица не растёт.
- Незавершённые публикации отправляются повторно при старте (`republish-outstanding-events-on-restart`) и по расписанию (`NotificationScheduledJobs.resubmitIncompletePublications`).

## Последствия

- Принятое уведомление не теряется при недоступной Kafka (проверяет `OutboxResilienceIT`).
- Возможны повторные публикации, поэтому потребители идемпотентны (ADR 0005).
- Контекст трассировки HTTP-запроса может не дойти до записи Kafka, потому что externalization асинхронна: `traceparent` ставит observation KafkaTemplate при отправке.
