# Как устроен сервис

Сервис принимает команду «уведомить получателя», формирует текст по шаблону и доставляет его по одному или нескольким каналам. Статус каждой доставки отслеживается и публикуется событиями.

## Модули

Луковая архитектура: зависимости направлены только внутрь. Направление проверяет ArchUnit (`ArchitectureTest`).

```
notification-domain          чистая Java: агрегаты, value objects, политики, доменные события
        ▲
notification-application     сценарии (use cases) и порты: port/in — что умеет сервис,
        ▲                    port/out — что ему нужно от внешнего мира. Из Spring только spring-tx
notification-infrastructure  адаптеры: REST, Kafka, JPA, SMTP, webhook, Thymeleaf, планировщик
        ▲
notification-presentation    Spring Boot приложение (пакет bootstrap): сборка сценариев,
                             конфигурация Kafka/безопасности, application*.yml, Liquibase
```

| Модуль | Ключевые пакеты |
|---|---|
| domain | `notification` (агрегат `Notification`), `delivery` (агрегат `Delivery`, `ChannelType`, адреса), `template`, `preferences`, `policy` (`RetryPolicy`, `QuietHoursPolicy`, `ChannelSelectionPolicy`, `NotificationStatusCalculator`) |
| application | `port.in` (`SendNotificationUseCase`, `ProcessDeliveryUseCase` …), `port.out` (`ChannelSender`, `RecipientResolver`, `TemplateRenderer` …), `service` (реализации сценариев) |
| infrastructure | `in.rest`, `in.messaging`, `in.events`, `in.scheduling`, `out.channel.*`, `out.messaging`, `out.persistence`, `out.template`, `out.recipient` |
| presentation | `bootstrap.config` (`UseCaseConfiguration`, `KafkaConfiguration`, `SecurityConfiguration`, `DeliveryListenerRegistrar`), `bootstrap.dev` (dev-токены) |

## Основные понятия

- **Notification (уведомление)** — один запрос клиента: получатель, шаблон или готовый текст, список каналов, категория, приоритет, время отправки.
- **Delivery (доставка)** — отправка уведомления по одному каналу. У каждой доставки свой статус, попытки и адрес. Доставки обновляются независимо друг от друга.
- **Template (шаблон)** — ключ + канал + локаль, с версиями. Активна одна версия. Хранится в БД, рендерится Thymeleaf.
- **Recipient preferences** — отписки по каналам и категориям, тихие часы, часовой пояс, локаль.
- **Category** — `TRANSACTIONAL` и `SYSTEM` (обязательные: отписка и тихие часы не действуют), `MARKETING` (учитывает отписки, тихие часы и лимиты частоты).
- **Priority** — `LOW`, `NORMAL`, `HIGH`. `HIGH` идёт в отдельные топики со своими потребителями.

Полный словарь — [glossary.md](glossary.md).

## Путь уведомления

```
 REST POST /api/v1/notifications          Kafka notification.send.requests.v1
            │                                         │
            └──────────────┬──────────────────────────┘
                           ▼
            SendNotificationService  (одна транзакция БД)
            1. идемпотентность: (clientId, Idempotency-Key)
            2. контакты: из запроса, недостающие — RecipientResolver
            3. выбор каналов: отписки, наличие контакта
            4. рендер шаблона для каждого канала
            5. дедупликация (если у шаблона задано окно)
            6. тихие часы / sendAt → время отправки
            7. INSERT notification + delivery
            8. outbox: сообщения в топики каналов + события статусов
                           │ commit
                           ▼
            Spring Modulith отправляет outbox в Kafka
                           │
                           ▼
   notification.delivery.<канал>[.high].v1   ──►  ChannelDeliveryListener (воркер канала)
                                                        │
                                                        ▼
                                          DeliveryProcessingService
                                          tx1: PENDING → SENDING (attempts+1)
                                          без tx: ChannelSender.send() → провайдер
                                          tx2: SENT | RETRY_SCHEDULED | FAILED + события
                                                        │
              ┌─────────────────────────────────────────┼──────────────────────────┐
              ▼                                         ▼                          ▼
           SENT: ack                    RETRY_SCHEDULED: запись в           FAILED: запись в
                                        <топик>-retry-<мс> с задержкой      <топик>-dlt (parking lot)
                           │
                           ▼
       notification.events.v1  (delivery.sent.v1, notification.completed.v1, …)
```

Статус уведомления пересчитывает `NotificationStatusListener` асинхронно, после каждого изменения статуса доставки.

## Статусы

**Доставка** (`DeliveryStatus`):

```
PENDING → SENDING → SENT → DELIVERED (если провайдер подтверждает)
                  ↘ RETRY_SCHEDULED → SENDING
                  ↘ FAILED (постоянная ошибка или исчерпаны попытки)
PENDING → SKIPPED (отписка, нет контакта) | CANCELLED
RETRY_SCHEDULED → CANCELLED
PENDING | RETRY_SCHEDULED → FAILED (запись ушла в DLT)
```

**Уведомление** (`NotificationStatus`) вычисляется из статусов доставок:

| Статус | Когда |
|---|---|
| `ACCEPTED` | ни одна доставка ещё не начиналась |
| `IN_PROGRESS` | есть начатые, но не все завершены |
| `COMPLETED` | все завершены, неудачных нет (`SKIPPED` и `CANCELLED` неудачей не считаются) |
| `PARTIALLY_FAILED` | есть и успешные, и неудачные |
| `FAILED` | все неудачные |
| `CANCELLED` | отменено до начала отправки |

## Надёжность

- **At-least-once.** Отправка провайдеру и запись статуса — разные системы, общей транзакции нет. Сервис выбирает повтор, а не потерю. `deliveryId` передаётся провайдеру как ключ идемпотентности (`Message-ID` письма, `Idempotency-Key` вебхука).
- **Transactional outbox** (Spring Modulith Event Publication Registry, таблица `event_publication`). Уведомление, доставки и сообщения для Kafka сохраняются в одной транзакции. Если Kafka недоступна, запрос всё равно принимается; незавершённые публикации отправляются повторно планировщиком.
- **Идемпотентность приёма.** Уникальная пара `(client_id, idempotency_key)` в БД. Повтор с тем же телом возвращает исходный результат, с другим телом — `422 IDEMPOTENCY_KEY_REUSED`.
- **Повторы.** Временная ошибка → retry-топик с экспоненциальной задержкой. Одна политика `notification.retry.*` для Kafka и для домена (счётчик попыток).
- **Parking lot.** Постоянная ошибка или исчерпанные попытки → `<топик>-dlt`, доставка `FAILED`. `FAILED` не перезапускается; для повтора отправьте уведомление заново с новым ключом.
- **Самовосстановление** (`NotificationScheduledJobs`, `FOR UPDATE SKIP LOCKED`, безопасно на нескольких экземплярах):
  - отложенные доставки (`sendAt`, тихие часы, лимит частоты) ставятся в очередь, когда подошло время;
  - доставки, зависшие в `SENDING` дольше `stale-sending-threshold` (экземпляр упал во время отправки), возвращаются в очередь;
  - доставки, поставленные в топик, но не обработанные за `overdue-retry-grace` (сообщение потеряно), ставятся заново;
  - незавершённые публикации outbox отправляются повторно.
- **Circuit breaker** на каждый канал (Resilience4j, `channel-<канал>`): пока он открыт, отправка не выполняется, доставка уходит в повтор.
- **Лимит параллелизма** на канал: `@ConcurrencyLimit` на `send()` (`max-concurrency`).

## Безопасность

- OAuth2 Resource Server (JWT): сервис не выдаёт токены и не является OAuth-клиентом, он только проверяет JWT вашего IdP — подпись (JWKS из `issuer-uri`), срок, `iss` и `aud` (`JWT_AUDIENCE`). Отправитель (`clientId`) берётся из claim `client_id`, затем `azp`, затем `sub`. Каждый клиент видит только свои уведомления и статистику.
- Права — scopes (см. [integration.md](integration.md#авторизация)).
- Лимит запросов на клиента (Resilience4j RateLimiter, в памяти экземпляра) → `429` с `Retry-After`.
- Персональные данные в логах маскируются. В событиях Kafka адресов нет.
- Переменные шаблона — данные пользователя: они экранируются в HTML и не выполняются как выражения.

## Технологии

Java 25, Spring Boot 4.1, Spring Framework 7 (`@Retryable`, `@ConcurrencyLimit`), Spring for Apache Kafka 4, Spring Modulith 2.1, PostgreSQL 17, Liquibase, Thymeleaf 3.1, Resilience4j, Micrometer + OpenTelemetry, Testcontainers. Обоснования — [adr/](adr).

> **Java 25 обязательна.** Kafka-слушатели работают на виртуальных потоках. На JDK 21 виртуальный поток, заблокированный внутри `synchronized` (это есть в Kafka-клиенте), занимает поток-носитель, и сервис зависает при остановке или ребалансе. Исправлено в JDK 24 (JEP 491).
