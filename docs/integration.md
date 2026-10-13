# Как подключить сервис к своему бэкенду

Сервис разворачивается как отдельный микросервис. Ваши сервисы общаются с ним тремя способами:

| Способ | Когда использовать |
|---|---|
| **REST** `POST /api/v1/notifications` | Синхронный ответ с `id` нужен сразу; простая интеграция |
| **Kafka** `notification.send.requests.v1` | Сервис-источник уже пишет в Kafka; нужна развязка по доступности |
| **События** `notification.events.v1` | Нужно знать, что письмо ушло, не ушло или уведомление завершено |

Оба входа (REST и Kafka) приводят к одному и тому же сценарию `SendNotificationUseCase`: правила, идемпотентность и статусы одинаковы.

Если нужен сервис со своими каналами или логикой, возьмите репозиторий как основу — см. [extending.md](extending.md).

## Авторизация

REST защищён OAuth2 (JWT). Отправитель — claim `client_id`, затем `azp`, затем `sub`. Клиент видит только свои уведомления.

| Scope | Доступ |
|---|---|
| `notifications:send` | `POST /notifications`, `/batch`, `/{id}/cancel` |
| `notifications:send-raw` | готовое содержимое без шаблона (поле `content`) |
| `notifications:read` | `GET /notifications`, `/{id}`, `/statistics` |
| `templates:manage` | `/api/v1/templates/**` |
| `preferences:manage` | `/api/v1/recipients/{id}/preferences` |

- **prod:** токены выдаёт ваш IdP. Сервис проверяет подпись по ключам IdP (`JWT_ISSUER_URI`), срок действия, `iss` и `aud`: в claim `aud` должно быть значение `JWT_AUDIENCE` (по умолчанию `notification-service`). Токены, выданные для других сервисов, отклоняются с `401`. Настройте в IdP audience (resource/API) для этого сервиса.
- **local:** `POST /dev/token {"clientId":"orders-service"}` выдаёт тестовый токен со всеми scopes.

## REST API

Полная спецификация — Swagger UI `/swagger-ui.html` (в `prod` выключен).

### Отправить уведомление

```http
POST /api/v1/notifications
Authorization: Bearer <token>
Idempotency-Key: order-42-shipped
Content-Type: application/json

{
  "recipient": {
    "id": "user-42",
    "contacts": { "email": "anna@example.com", "phone": "+77011234567" }
  },
  "template": { "key": "order-shipped", "locale": "ru-KZ", "variables": { "orderId": "ORD-42" } },
  "channels": ["EMAIL", "SMS"],
  "category": "TRANSACTIONAL",
  "priority": "HIGH",
  "sendAt": "2030-01-01T09:00:00+05:00",
  "metadata": { "orderId": "ORD-42" }
}
```

| Поле | Обязательно | Описание |
|---|---|---|
| `recipient.id` | да | ваш идентификатор пользователя (до 100 символов) |
| `recipient.contacts` | нет | `email`, `phone` (E.164), `telegramChatId`, `deviceToken`, `webhookUrl`. Недостающие запрашиваются у сервиса пользователей, если он подключён |
| `template` | одно из двух | `key`, `locale` (BCP 47, необязательно), `variables` |
| `content` | одно из двух | `subject`, `text`, `html` — готовый текст, нужен scope `notifications:send-raw` |
| `channels` | да | `EMAIL`, `SMS`, `TELEGRAM`, `WHATSAPP`, `PUSH`, `WEBHOOK` и любые добавленные |
| `category` | да | `TRANSACTIONAL`, `SYSTEM`, `MARKETING` |
| `priority` | нет | `LOW`, `NORMAL` (по умолчанию), `HIGH` |
| `sendAt` | нет | отложенная отправка, ISO 8601 со смещением |
| `metadata` | нет | до 20 строковых пар; хранится вместе с уведомлением |

Ответ `202 Accepted`, заголовок `Location: /api/v1/notifications/{id}`:

```json
{ "id": "0199…", "status": "ACCEPTED", "outcome": "CREATED" }
```

`outcome`: `CREATED` — новое уведомление; `REPLAYED` — повтор с тем же ключом (плюс заголовок `Idempotent-Replayed: true`); `DEDUPLICATED` — такое же уведомление уже отправлялось в окне дедупликации шаблона.

**Правила для клиента:**

- `Idempotency-Key` обязателен. Используйте ключ, привязанный к бизнес-событию (`order-42-shipped`), и повторяйте запрос с тем же ключом при таймауте или `5xx`.
- Тот же ключ с другим телом → `422 IDEMPOTENCY_KEY_REUSED`.
- Ключи хранятся `notification.delivery.idempotency-key-retention` (по умолчанию 7 дней в `application.yml`).

### Остальные эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/notifications/batch` | до 500 элементов, ответ `207 Multi-Status` с кодом по каждому. Ключ элемента — его `idempotencyKey` или `<Idempotency-Key>:<индекс>` |
| `GET` | `/api/v1/notifications/{id}` | статус и разбивка по доставкам (канал, статус, попытки, замаскированный адрес, `simulated`) |
| `GET` | `/api/v1/notifications` | поиск: `recipientId`, `status`, `channel`, `from`, `to`, `page`, `size` |
| `POST` | `/api/v1/notifications/{id}/cancel` | отмена, пока ни одна доставка не начата (`ACCEPTED`), иначе `409` |
| `GET` | `/api/v1/statistics` | счётчики доставок: `from`, `to` (по умолчанию 7 дней, максимум 92), `channel`, `granularity` = `HOUR`/`DAY` |
| `GET/POST/PUT` | `/api/v1/templates…` | шаблоны, версии, активация, предпросмотр — см. [extending.md](extending.md#шаблоны) |
| `GET/PUT/PATCH` | `/api/v1/recipients/{id}/preferences` | отписки, тихие часы, часовой пояс, локаль |

### Ошибки

RFC 9457 Problem Details с машинно-читаемым `errorCode`:

```json
{ "type": "urn:problem-type:notification:missing-template-variables", "title": "Missing template variables",
  "status": 422, "detail": "…", "errorCode": "MISSING_TEMPLATE_VARIABLES", "missingVariables": ["orderId"] }
```

| HTTP | Примеры `errorCode` | Что делать |
|---|---|---|
| 400 | `REQUEST_INVALID`, `VALIDATION_FAILED` | исправить запрос |
| 401 / 403 | — | токен или scope |
| 404 | `NOTIFICATION_NOT_FOUND`, `TEMPLATE_NOT_FOUND` | — |
| 409 | `IDEMPOTENT_REQUEST_IN_PROGRESS`, `NOTIFICATION_NOT_CANCELLABLE`, `CONCURRENT_MODIFICATION` | повторить позже (первый) или не повторять |
| 422 | `IDEMPOTENCY_KEY_REUSED`, `MISSING_TEMPLATE_VARIABLES`, `TEMPLATE_NOT_AVAILABLE`, `CHANNEL_NOT_AVAILABLE`, `NO_DELIVERABLE_CHANNEL`, `RAW_CONTENT_NOT_ALLOWED` | не повторять: бизнес-правило |
| 429 | `RATE_LIMIT_EXCEEDED` | повторить после `Retry-After` |
| 500 | `INTERNAL_ERROR` | повторить с тем же `Idempotency-Key` |

## Kafka: отправка команд

Топик `notification.send.requests.v1`. Формат — CloudEvents 1.0, binary mode: атрибуты в заголовках, данные — JSON того же вида, что тело REST.

| Заголовок | Значение |
|---|---|
| `ce_specversion` | `1.0` |
| `ce_type` | `kz.company.notification.send.v1` |
| `ce_source` | идентификатор отправителя, например `urn:orders-service` — это `clientId` |
| `ce_id` | уникальный id события; ключ идемпотентности, если в данных нет `idempotencyKey` |
| `content-type` | `application/json` |

Ключ записи — `recipient.id` (порядок сообщений одного получателя).

```java
ProducerRecord<String, String> record = new ProducerRecord<>("notification.send.requests.v1", "user-42", """
        {"recipient":{"id":"user-42","contacts":{"email":"anna@example.com"}},
         "template":{"key":"order-shipped","variables":{"orderId":"ORD-42"}},
         "channels":["EMAIL"],"category":"TRANSACTIONAL","idempotencyKey":"order-42-shipped"}
        """);
record.headers()
        .add("ce_specversion", "1.0".getBytes(UTF_8))
        .add("ce_type", "kz.company.notification.send.v1".getBytes(UTF_8))
        .add("ce_source", "urn:orders-service".getBytes(UTF_8))
        .add("ce_id", UUID.randomUUID().toString().getBytes(UTF_8))
        .add("content-type", "application/json".getBytes(UTF_8));
producer.send(record);
```

- Готовое содержимое (`content`) принимается только от `ce_source` из `notification.kafka.trusted-sources`.
- Невалидное или отклонённое бизнес-правилами сообщение сразу уходит в `notification.send.requests.v1-dlt`. Причина — в заголовках `kafka_dlt-exception-*`.
- Временные ошибки (БД недоступна и т.п.) повторяются на месте: до 5 раз, задержка 1 → 30 с.
- Через Kafka ответа с `id` нет. Связать уведомление со своим событием можно через `metadata` или события статусов.

## Kafka: события статусов

Топик `notification.events.v1`, ключ — `notificationId`, CloudEvents binary mode (`ce_type`, `ce_id`, `ce_source=urn:kz.company:notification-service`, `ce_time`, `ce_subject`). Значение — JSON события.

| `ce_type` | Когда | Поля данных |
|---|---|---|
| `kz.company.notification.notification.accepted.v1` | уведомление принято | `notificationId`, `clientId`, `recipientId`, `category`, `priority`, `channels`, `occurredAt` |
| `…notification.completed.v1` | все доставки завершены | `notificationId`, `status` (`COMPLETED`/`PARTIALLY_FAILED`/`FAILED`), `occurredAt` |
| `…notification.cancelled.v1` | отменено | `notificationId`, `occurredAt` |
| `…delivery.scheduled.v1` | доставка создана | `deliveryId`, `notificationId`, `recipientId`, `channel`, `priority`, `scheduledFor`, `status` |
| `…delivery.sent.v1` | провайдер принял | `deliveryId`, `notificationId`, `channel`, `attempt`, `providerMessageId`, `simulated`, `status` |
| `…delivery.retry-scheduled.v1` | временная ошибка | `…`, `attempt`, `nextAttemptAt`, `reason` |
| `…delivery.failed.v1` | окончательная ошибка | `…`, `attempts`, `reason`, `permanent` |
| `…delivery.skipped.v1` | отписка или нет контакта | `…`, `reason` |
| `…delivery.cancelled.v1` | отменена | `deliveryId`, `notificationId`, `channel` |

Во всех событиях доставки есть `occurredAt` и `status`. Адресов получателя в событиях нет.

```java
@KafkaListener(topics = "notification.events.v1", groupId = "orders-service.notification-events")
void on(ConsumerRecord<String, String> record) {
    String type = new String(record.headers().lastHeader("ce_type").value(), UTF_8);
    if (type.equals("kz.company.notification.notification.completed.v1")) {
        JsonNode event = jsonMapper.readTree(record.value());
        markOrderNotified(event.get("notificationId").asString(), event.get("status").asString());
    }
}
```

Доставка событий — at-least-once: обрабатывайте их идемпотентно (например, по `ce_id`). Контракты в формате AsyncAPI — [asyncapi.yaml](asyncapi.yaml).

## Вебхуки: приём на своей стороне

Канал `WEBHOOK` делает `POST` JSON на `recipient.contacts.webhookUrl`:

```json
{ "deliveryId": "…", "notificationId": "…", "recipientId": "user-42",
  "subject": "…", "text": "…", "html": null, "timestamp": 1760380000 }
```

Заголовки: `Idempotency-Key` и `X-Notification-Delivery-Id` (= `deliveryId`), `X-Notification-Signature: t=<unix seconds>,v1=<hex>`. Подпись — `HMAC-SHA256(secret, t + "." + body)`, секрет — `WEBHOOK_SIGNING_SECRET`.

```java
boolean verify(String header, String body, String secret) throws GeneralSecurityException {
    Map<String, String> parts = Arrays.stream(header.split(","))
            .map(part -> part.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    long timestamp = Long.parseLong(parts.get("t"));
    if (Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) {
        return false; // защита от повторной отправки старого запроса
    }
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
    String expected = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(UTF_8)));
    return MessageDigest.isEqual(expected.getBytes(UTF_8), parts.get("v1").getBytes(UTF_8));
}
```

Ответ `2xx` — успех. `408`, `425`, `429`, `5xx` и сетевые ошибки — повтор. Остальные `4xx` — окончательная ошибка. Один вызов может прийти повторно: отбрасывайте дубли по `Idempotency-Key`.

## Сервис пользователей (необязательно)

Если клиенты не передают контакты, сервис может запрашивать их сам. Задайте `RECIPIENT_DIRECTORY_URL` (и `RECIPIENT_DIRECTORY_TOKEN`). Ваш сервис должен отвечать на:

```http
GET {base-url}/api/v1/recipients/{recipientId}/contacts
Authorization: Bearer <token>

200 {"email": "anna@example.com", "phone": "+77011234567", "telegramChatId": "123456"}
404 — получатель не найден
```

Контакты из запроса имеют приоритет над справочником. Справочник вызывается, только если для запрошенных каналов чего-то не хватает. Ответ, отличный от `200` и `404`, — временная ошибка: запрос отклоняется целиком, и клиент повторяет его с тем же ключом.

## Чек-лист интеграции

1. Получить у владельца IdP клиента с `client_id` и нужными scopes.
2. Завести шаблоны (`POST /api/v1/templates`) для своих ключей, каналов и локалей.
3. Отправлять запросы с `Idempotency-Key`, привязанным к бизнес-событию.
4. Повторять при `429`, `5xx`, `409 IDEMPOTENT_REQUEST_IN_PROGRESS` и таймаутах; не повторять при `422`.
5. Если важен итог — подписаться на `notification.events.v1` или опрашивать `GET /notifications/{id}`.
6. Для вебхуков — проверять подпись и отбрасывать дубли.
