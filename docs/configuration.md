# Настройки и топики

Конфигурация — `notification-presentation/src/main/resources/application.yml` и профили:

| Профиль | Назначение |
|---|---|
| (базовый) | значения по умолчанию; секреты только из переменных окружения |
| `local` | docker compose: один брокер (3 партиции, RF 1), Mailpit, dev-токены, retry 5 с → 1 мин, DEBUG-логи |
| `prod` | JWT от IdP (`JWT_ISSUER_URI`), Swagger выключен, actuator на порту 8081, вебхуки только HTTPS |
| `test` | интеграционные тесты: 1 партиция, retry 1 → 2 с, частые тики планировщика |

> Запуск без `local` или `prod` не стартует: нужен либо `notification.security.dev-tokens.enabled=true`, либо `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

## Переменные окружения

| Переменная | Назначение | По умолчанию |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL | `jdbc:postgresql://localhost:5432/notifications`, `notifications`/`notifications` |
| `DB_POOL_SIZE` | размер пула Hikari | `20` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka | `localhost:9092` |
| `KAFKA_TRUSTED_SOURCES` | `ce_source` через запятую, которым разрешён `content` без шаблона | — |
| `JWT_ISSUER_URI` | issuer IdP (профиль `prod`) | — |
| `JWT_AUDIENCE` | значение, которое должно быть в claim `aud` токена (профиль `prod`) | `notification-service` |
| `SMTP_HOST`, `SMTP_PORT` | SMTP-сервер | `localhost`, `1025` |
| `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` | аутентификация SMTP | —, —, `false` |
| `SMTP_STARTTLS`, `SMTP_SSL` | 587 + STARTTLS или 465 + SSL | `false`, `false` |
| `SMTP_CONNECT_TIMEOUT_MS`, `SMTP_READ_TIMEOUT_MS`, `SMTP_WRITE_TIMEOUT_MS` | таймауты SMTP | `5000`, `10000`, `10000` |
| `MAIL_FROM`, `MAIL_FROM_NAME`, `MAIL_REPLY_TO` | отправитель | `no-reply@example.com`, `Notifications`, — |
| `MAIL_MESSAGE_ID_DOMAIN` | домен в `Message-ID: <deliveryId@домен>` | `notification.local` |
| `MAIL_UNSUBSCRIBE_URL_TEMPLATE` | `List-Unsubscribe` для MARKETING; подстановки `{recipientId}`, `{deliveryId}` | — |
| `WEBHOOK_SIGNING_SECRET` | секрет HMAC-подписи вебхуков | — (без него запросы не подписываются) |
| `RECIPIENT_DIRECTORY_URL`, `RECIPIENT_DIRECTORY_TOKEN` | сервис пользователей | — (контакты только из запроса) |
| `CLIENT_RATE_LIMIT_PER_SECOND` | лимит запросов клиента в секунду на экземпляр | `100` |
| `TRACING_SAMPLING_PROBABILITY` | доля трассируемых запросов | `0.1` |

Примеры SMTP для Exchange, Postfix, Microsoft 365 и Google Workspace — в [README](../README.md#email).

### docker compose

`docker-compose.yml` читает `.env` рядом с собой (пример — [`.env.example`](../.env.example)): SMTP, `WEBHOOK_SIGNING_SECRET`, настройки админки (`FRONTEND_*`) и провайдеры каналов `<КАНАЛ>_PROVIDER` (`EMAIL_PROVIDER`, `SMS_PROVIDER`, `TELEGRAM_PROVIDER`, `WHATSAPP_PROVIDER`, `PUSH_PROVIDER`, `WEBHOOK_PROVIDER`). Провайдер передаётся сервису как `NOTIFICATION_CHANNELS_<КАНАЛ>_PROVIDER` и админке в `CHANNEL_PROVIDERS`. Настройки админки — в [frontend.md](frontend.md#настройки).

## `notification.*`

### Общие и повторы (`NotificationProperties`)

| Свойство | По умолчанию | Описание |
|---|---|---|
| `notification.default-locale` | `ru` | последнее звено fallback локали шаблона: `ru-KZ → ru → default` |
| `notification.retry.max-attempts` | `5` | всего попыток, включая первую |
| `notification.retry.initial-delay` | `30s` | задержка после первой неудачи |
| `notification.retry.multiplier` | `2.0` | множитель задержки |
| `notification.retry.max-delay` | `30m` | потолок задержки |
| `notification.retry.jitter` | `0.2` | разброс ±20% (только для доменного `nextAttemptAt`) |

Одна политика задаёт и retry-топики Kafka, и счётчик попыток в домене. Значения по умолчанию дают задержки 30 с, 1 мин, 2 мин, 4 мин.

### Доставка (`NotificationProperties.Delivery`)

| Свойство | По умолчанию | Описание |
|---|---|---|
| `notification.delivery.stale-sending-threshold` | `5m` | доставка в `SENDING` дольше этого срока считается зависшей и возвращается в очередь |
| `notification.delivery.overdue-retry-grace` | `10m` | доставка, поставленная в топик, но не обработанная дольше этого срока, ставится заново |
| `notification.delivery.batch-size` | `200` | сколько строк планировщик обрабатывает за проход |
| `notification.delivery.idempotency-key-retention` | `7d` (`24h` в коде) | срок хранения ключей идемпотентности |
| `notification.delivery.rate-limits.<канал>.max` | — | не больше `max` отправок получателю по каналу… |
| `notification.delivery.rate-limits.<канал>.window` | — | …за окно; лишние откладываются, а не отбрасываются |
| `notification.delivery.rate-limits.<канал>.categories` | — | к каким категориям применяется; если не задано — `MARKETING` |

```yaml
notification:
  delivery:
    rate-limits:
      sms:
        max: 5
        window: 1h
        categories: [MARKETING]
```

### Kafka (`NotificationKafkaProperties`)

| Свойство | По умолчанию | Описание |
|---|---|---|
| `notification.kafka.source` | `urn:kz.company:notification-service` | `ce_source` исходящих событий |
| `notification.kafka.trusted-sources` | — | см. `KAFKA_TRUSTED_SOURCES` |
| `notification.kafka.consumer-group-prefix` | `notification-service` | префикс consumer group |
| `notification.kafka.send-requests-topic` | `notification.send.requests.v1` | входящие команды |
| `notification.kafka.events-topic` | `notification.events.v1` | события статусов |
| `notification.kafka.delivery-topic-prefix` | `notification.delivery` | префикс топиков доставок |
| `notification.kafka.delivery-topic-version` | `v1` | версия в имени топика доставок |
| `notification.kafka.dead-letter-suffix` | `-dlt` | суффикс DLT |
| `notification.kafka.partitions` | `12` | партиций у создаваемых топиков (`local` — 3) |
| `notification.kafka.replication-factor` | `3` | (`local` — 1) |
| `notification.kafka.min-insync-replicas` | `2` | не больше фактора репликации |
| `notification.kafka.retention` | `7d` | хранение основных топиков |
| `notification.kafka.dead-letter-retention` | `30d` | хранение DLT (parking lot) |

Стандартные `spring.kafka.*` тоже действуют: в базовом `application.yml` producer настроен на `acks=all` с идемпотентностью, consumer — на `read_committed` без авто-коммита, подтверждение после каждой записи.

### Каналы (`notification.channels.<канал>.*`)

Общие для всех каналов:

| Свойство | По умолчанию | Описание |
|---|---|---|
| `enabled` | `false` (в `application.yml` все `true`) | выключенный канал не принимается API, топики и воркеры для него не создаются |
| `provider` | см. ниже | какая реализация провайдера активна |
| `max-concurrency` | у каждого канала своё | сколько отправок по каналу одновременно на экземпляр |
| `concurrency` | `3` | потребителей полосы NORMAL (не больше числа партиций) |
| `high-priority-concurrency` | `6` | потребителей полосы HIGH (не больше числа партиций) |
| `stub.failure-probability` | `0.0` | только для заглушек: вероятность временной ошибки |
| `stub.permanent-failure-probability` | `0.0` | только для заглушек: вероятность постоянной ошибки |

| Канал | Провайдеры | `max-concurrency` | Дополнительно |
|---|---|---|---|
| `email` | `smtp` (по умолчанию), `logging` | 20 | `from`, `from-name`, `reply-to`, `message-id-domain`, `unsubscribe-url-template`; подключение — `spring.mail.*` |
| `sms` | `logging` | 10 | — |
| `telegram` | `logging` | 20 | — |
| `whatsapp` | `logging` | 10 | — |
| `push` | `logging` | 50 | — |
| `webhook` | `http` (по умолчанию), `logging` | 20 | `signing-secret`, `require-https`, `connect-timeout` (5s), `read-timeout` (10s), `network-retries` (2), `network-retry-delay` (200 мс) |

`logging` — заглушка: пишет в лог, что было бы отправлено, и возвращает успех с `simulated=true`. Подключение реального провайдера — [extending.md](extending.md#1-подключить-реального-провайдера-к-существующему-каналу).

### Планировщик (`SchedulingProperties`)

| Свойство | По умолчанию | Задача |
|---|---|---|
| `notification.scheduler.enabled` | `true` | выключает все задачи (например, на отдельных read-only экземплярах) |
| `notification.scheduler.dispatch-interval` | `5s` | отложенные доставки, у которых подошло время |
| `notification.scheduler.recovery-interval` | `1m` | зависшие в `SENDING` и потерянные сообщения |
| `notification.scheduler.idempotency-cleanup-interval` | `1h` | удаление старых ключей идемпотентности |
| `notification.scheduler.outbox-resubmit-interval` | `1m` | повторная отправка незавершённых публикаций outbox… |
| `notification.scheduler.outbox-resubmit-after` | `2m` | …старше этого срока |

### Прочее

| Свойство | По умолчанию | Описание |
|---|---|---|
| `notification.recipient-directory.base-url` | — | сервис пользователей; пусто — контакты только из запроса |
| `notification.recipient-directory.bearer-token` | — | токен для него |
| `notification.recipient-directory.connect-timeout` / `read-timeout` | `2s` / `3s` | |
| `notification.security.dev-tokens.enabled` | `false` | `POST /dev/token` и локальная проверка JWT. **Никогда не включать в prod** |
| `resilience4j.circuitbreaker.configs.channels.*` | окно 20, порог 50%, открыт 30 с | circuit breaker каналов (`channel-<канал>`) |
| `resilience4j.ratelimiter.configs.clients.*` | 100 в секунду | лимит запросов клиента (`client-<clientId>`) |
| `spring.modulith.events.kafka.enable-json` | `false` | **не менять**: иначе Modulith сериализует в Kafka обёртку вместо payload |

## Топики Kafka

Топики создаёт сервис при старте (бины `NewTopic`, `KafkaConfiguration`). Автосоздание на брокере можно выключить.

| Топик | Ключ | Кто пишет → кто читает | Назначение |
|---|---|---|---|
| `notification.send.requests.v1` | recipientId | ваши сервисы → `SendRequestListener` | команды на отправку |
| `notification.send.requests.v1-dlt` | как в исходном | сервис | невалидные или отклонённые команды |
| `notification.events.v1` | notificationId | сервис → ваши сервисы | события статусов |
| `notification.delivery.<канал>.v1` | recipientId | outbox → воркер канала | доставки NORMAL/LOW |
| `notification.delivery.<канал>.high.v1` | recipientId | outbox → воркер канала | доставки HIGH |
| `notification.delivery.<канал>[.high].v1-retry-<мс>` | recipientId | сервис → воркер | повторы с задержкой; по топику на каждую задержку |
| `notification.delivery.<канал>[.high].v1-dlt` | recipientId | сервис → `DeliveryDeadLetterHandler` | parking lot (хранение 30 дней) |

`<канал>` — код в нижнем регистре: `email`, `sms`, `telegram`, `whatsapp`, `push`, `webhook`. Топики `notification.delivery.*` — внутренние: формат их сообщений не является публичным контрактом.

Consumer groups: `notification-service.send-requests`, `notification-service.delivery.<канал>` и `notification-service.delivery.<канал>.high` (retry- и DLT-топики — с суффиксами `-retry-<мс>` и `-dlt`).

**Сколько это потребителей.** При N каналах и `max-attempts: 5` получается N × 2 полосы × (основной + 4 retry + DLT) контейнеров, каждый с `concurrency` потребителями. С шестью каналами это ~70 контейнеров. Выключайте неиспользуемые каналы (`enabled: false`) — это сразу убирает их топики и потребителей.

## База данных

Схема создаётся Liquibase (`db/changelog`), Hibernate только проверяет её (`ddl-auto: validate`). Применённые changeset'ы не меняются — только новые файлы.

| Таблица | Содержимое |
|---|---|
| `notification` | уведомления, ключи идемпотентности, дедупликация |
| `delivery` | доставки по каналам: статус, попытки, адрес, отрендеренное сообщение |
| `template`, `template_revision` | шаблоны и их версии |
| `recipient_preferences` | отписки, тихие часы, часовой пояс, локаль |
| `event_publication` | outbox Spring Modulith |

Seed `100-seed-templates.sql` (label `seed`) заводит примеры: `welcome`, `password-reset`, `email-verification`, `order-shipped`. Чтобы не загружать их в prod, запускайте Liquibase с `spring.liquibase.label-filter: "!seed"`.

## Наблюдаемость

| Что | Где |
|---|---|
| Health, liveness/readiness | `/actuator/health`, `/actuator/health/{liveness,readiness}`; индикаторы `mail` и circuit breakers |
| Метрики Prometheus | `/actuator/prometheus`; `notification.channel.send{channel,outcome}` — длительность и итог отправки |
| Трассировка | Micrometer Tracing → OpenTelemetry; observation на Kafka-шаблоне и слушателях |
| Логи | JSON (ECS) в stdout; email и телефоны маскируются |
