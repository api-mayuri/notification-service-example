# Notification Service

Сервис нотификаций. Он принимает запросы через REST API или Kafka, формирует текст по шаблонам из БД и доставляет его по каналам: email, SMS, Telegram, WhatsApp, push и webhook. Статус каждой доставки отслеживается.

- **Архитектура:** луковая, по DDD. Модули Maven: `domain → application → infrastructure → bootstrap`.
- **Стек:** Java 25, Spring Boot 4.1, PostgreSQL, Liquibase, Apache Kafka 4 (KRaft), Spring Modulith (transactional outbox), Resilience4j, Thymeleaf.
- **Надёжность:** at-least-once, идемпотентность, повторы через retry-топики, dead letter topics.

| Документ | Что внутри |
|---|---|
| [docs/plan.md](docs/plan.md) | Модули, пакеты, схема БД и брокера |
| [docs/adr/](docs/adr) | Архитектурные решения |
| [docs/asyncapi.yaml](docs/asyncapi.yaml) | Контракты Kafka (AsyncAPI 3.0) |
| [docs/glossary.md](docs/glossary.md) | Единый язык |
| [docs/how-to-add-channel.md](docs/how-to-add-channel.md) | Как добавить канал или заменить заглушку провайдером |
| [docs/progress.md](docs/progress.md) | Состояние работ и открытые вопросы |
| `/swagger-ui.html` | OpenAPI 3.1 (в prod выключен) |

## Запуск за 5 минут

Нужен только Docker (Compose v2).

```bash
docker compose up --build
```

Поднимутся PostgreSQL, Kafka (KRaft, без ZooKeeper), Kafka UI, Mailpit и сам сервис с профилем `local`.

| Что | Адрес |
|---|---|
| API и Swagger UI | http://localhost:8080/swagger-ui.html |
| Mailpit (входящие письма) | http://localhost:8025 |
| Kafka UI | http://localhost:8088 |
| Kafka с хоста | `localhost:29092` |
| Health | http://localhost:8080/actuator/health |

### 1. Получить токен

В профиле `local` сервис сам выпускает тестовые JWT. Это только для разработки, в prod эндпоинта нет.

```bash
TOKEN=$(curl -s -X POST localhost:8080/dev/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"orders-service"}' | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
```

По умолчанию токен получает все scopes. Можно передать свои: `{"clientId":"x","scopes":["notifications:read"]}`.

### 2. Отправить письмо

```bash
curl -i -X POST localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Idempotency-Key: demo-1' \
  -H 'Content-Type: application/json' \
  -d '{
        "recipient": {"id": "user-42", "contacts": {"email": "anna@example.com"}},
        "template": {"key": "welcome", "locale": "ru-KZ", "variables": {"name": "Анна"}},
        "channels": ["EMAIL"],
        "category": "TRANSACTIONAL"
      }'
```

Ответ — `202 Accepted` с заголовком `Location: /api/v1/notifications/{id}`. Письмо «Добро пожаловать, Анна!» появится в Mailpit (http://localhost:8025).

Повторите тот же запрос: сервис вернёт тот же `id` с заголовком `Idempotent-Replayed: true`, дубля не будет. Если изменить тело при том же ключе, вернётся `422 IDEMPOTENCY_KEY_REUSED`.

### 3. Посмотреть статус

```bash
curl -s localhost:8080/api/v1/notifications/<id> -H "Authorization: Bearer $TOKEN"
```

В ответе есть статус уведомления (`ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `PARTIALLY_FAILED`, `FAILED`, `CANCELLED`) и разбивка по доставкам: канал, статус, попытки, замаскированный адрес и флаг `simulated`.

### 4. Несколько каналов, пакет, отмена

```bash
# SMS и Telegram — логирующие заглушки: в ответе simulated=true
curl -s -X POST localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-2' -H 'Content-Type: application/json' \
  -d '{"recipient":{"id":"user-42","contacts":{"phone":"+77011234567","telegramChatId":"123456"}},
       "template":{"key":"order-shipped","variables":{"orderId":"ORD-42"}},
       "channels":["SMS","TELEGRAM"],"category":"TRANSACTIONAL","priority":"HIGH"}'

# Отложенная отправка и отмена до неё
curl -s -X POST localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-3' -H 'Content-Type: application/json' \
  -d '{"recipient":{"id":"user-42","contacts":{"email":"anna@example.com"}},
       "template":{"key":"welcome","variables":{"name":"Анна"}},
       "channels":["EMAIL"],"category":"MARKETING","sendAt":"2030-01-01T09:00:00+05:00"}'
curl -s -X POST localhost:8080/api/v1/notifications/<id>/cancel -H "Authorization: Bearer $TOKEN"

# Пакет до 500 элементов: ответ 207 Multi-Status с кодом по каждому элементу
curl -s -X POST localhost:8080/api/v1/notifications/batch \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: batch-1' -H 'Content-Type: application/json' \
  -d '{"items":[
        {"recipient":{"id":"u1","contacts":{"email":"u1@example.com"}},"template":{"key":"welcome","variables":{"name":"U1"}},"channels":["EMAIL"],"category":"TRANSACTIONAL"},
        {"recipient":{"id":"u2","contacts":{"email":"u2@example.com"}},"template":{"key":"welcome","variables":{}},"channels":["EMAIL"],"category":"TRANSACTIONAL"}
      ]}'
```

### 5. Отправить команду через Kafka

Входящий топик — `notification.send.requests.v1`. Формат: CloudEvents 1.0 в binary mode, атрибуты передаются в заголовках `ce_*`, данные — JSON того же вида, что и в REST.

```bash
docker compose exec kafka bash -c '
cat <<EOF | /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic notification.send.requests.v1 \
  --property parse.key=true --property parse.headers=true \
  --property headers.delimiter="|" --property key.separator=";"
ce_id:evt-1,ce_source:urn:local:demo-producer,ce_type:kz.company.notification.send.v1,ce_specversion:1.0,content-type:application/json|user-42;{"recipient":{"id":"user-42","contacts":{"email":"anna@example.com"}},"template":{"key":"order-shipped","variables":{"orderId":"ORD-77"}},"channels":["EMAIL"],"category":"TRANSACTIONAL"}
EOF'
```

- Отправитель (client id) — это `ce_source`.
- Ключ идемпотентности — поле `idempotencyKey` из данных, а если его нет — `ce_id`.
- Готовое содержимое без шаблона принимается только от источников из `notification.kafka.trusted-sources`.
- Невалидное сообщение уходит в `notification.send.requests.v1-dlt`.

События статусов публикуются в `notification.events.v1` с ключом `notificationId`. Их видно в Kafka UI.

## Сборка и тесты

```bash
./mvnw verify
```

Нужны JDK 25 и Docker: интеграционные тесты поднимают PostgreSQL и Kafka в Testcontainers.

| Уровень | Где | Что проверяется |
|---|---|---|
| Домен | `notification-domain/src/test` | Инварианты, машина состояний, политики. Без Spring |
| Application | `notification-application/src/test` | Сценарии на in-memory фейках портов |
| Адаптеры | `notification-infrastructure/src/test` | SMTP (GreenMail), webhook (WireMock), Thymeleaf, маппинг, Kafka-воркер |
| REST-контракт | `…/bootstrap/web` | `@WebMvcTest`: коды, заголовки, Problem Details, scopes |
| Интеграция | `…/bootstrap/integration/*IT` | REST/Kafka → БД → Kafka → канал → событие, повторы и DLT, идемпотентность, отказ брокера, `SKIP LOCKED` |
| Архитектура | `…/bootstrap/architecture` | ArchUnit: направление зависимостей, отсутствие Spring в домене, имена |

Для domain и application JaCoCo требует покрытие строк не ниже 90% (`jacoco:check` в фазе `verify`).

## Подключение корпоративной почты

Email — основной рабочий канал. Письма уходят через `JavaMailSender`; это multipart (текст + HTML) в UTF-8, с заголовками `Message-ID` (`<deliveryId@домен>`) и `List-Unsubscribe` для marketing-писем.

Настройки передаются только через переменные окружения. Пример `.env` для `docker compose --env-file .env up`:

```dotenv
SMTP_HOST=smtp.company.kz
SMTP_PORT=587
SMTP_USERNAME=notifications@company.kz
SMTP_PASSWORD=change-me
SMTP_AUTH=true
SMTP_STARTTLS=true        # 587 + STARTTLS
SMTP_SSL=false            # true для 465 (SMTPS), тогда SMTP_STARTTLS=false
SMTP_CONNECT_TIMEOUT_MS=5000
SMTP_READ_TIMEOUT_MS=10000
SMTP_WRITE_TIMEOUT_MS=10000
MAIL_FROM=notifications@company.kz
MAIL_FROM_NAME=Компания
MAIL_REPLY_TO=support@company.kz
MAIL_MESSAGE_ID_DOMAIN=company.kz
MAIL_UNSUBSCRIBE_URL_TEMPLATE=https://company.kz/unsubscribe?recipient={recipientId}
```

| Сервер | Порт и шифрование | Замечания |
|---|---|---|
| Microsoft Exchange (on-premises) | 587, STARTTLS | Нужен коннектор приёма с правом relay или учётная запись с правом «Send As» |
| Postfix / Exim (свой релей) | 25 или 587, STARTTLS | Для доверенной подсети часто `SMTP_AUTH=false` |
| Microsoft 365 | smtp.office365.com:587, STARTTLS | Basic auth для SMTP AUTH отключается Microsoft. Нужен OAuth2 (XOAUTH2), см. ниже |
| Google Workspace | smtp.gmail.com:587, STARTTLS или 465, SSL | Пароль приложения или SMTP relay (smtp-relay.gmail.com). Для OAuth2 см. ниже |

Microsoft 365 и Google Workspace могут требовать OAuth2 вместо пароля. Для этого предусмотрена отдельная будущая реализация `EmailProvider` (`provider=smtp-oauth2`): она будет получать токен у IdP и передавать его в `mail.smtp.auth.mechanisms=XOAUTH2`. Сейчас она не реализована, и `EmailChannelSender` менять не придётся.

Ошибки SMTP классифицируются так:

- **Временные** (повтор с backoff): `4xx`, сетевые ошибки, ошибки аутентификации.
- **Постоянные** (сразу FAILED): `5xx`, например нет ящика или отказ в релее.

Health-индикатор `mail` в `/actuator/health` проверяет соединение с SMTP-сервером.

## Прочие переменные окружения

| Переменная | Назначение |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka |
| `JWT_ISSUER_URI` | Issuer корпоративного IdP (профиль `prod`) |
| `WEBHOOK_SIGNING_SECRET` | Секрет HMAC-SHA256 для подписи вебхуков |
| `RECIPIENT_DIRECTORY_URL`, `RECIPIENT_DIRECTORY_TOKEN` | Сервис пользователей (контакты по `recipientId`) |
| `KAFKA_TRUSTED_SOURCES` | `ce_source` через запятую, которым разрешено содержимое без шаблона |
| `CLIENT_RATE_LIMIT_PER_SECOND` | Лимит запросов клиента в секунду на экземпляр |
| `TRACING_SAMPLING_PROBABILITY` | Доля трассируемых запросов |

## Гарантии доставки: почему at-least-once

Exactly-once до внешнего провайдера недостижимо. Отправка письма или SMS и запись статуса в БД — два разных действия в разных системах, и общей транзакции между ними нет. Если экземпляр упадёт после того, как SMTP-сервер принял письмо, но до записи `SENT`, мы не узнаем, дошло ли письмо. Остаётся выбор: повторить (возможен дубль) или не повторять (возможна потеря). Сервис выбирает повтор, то есть at-least-once.

Как система защищается от дублей:

1. **Идемпотентность приёма.** Ключ уникален в паре `(client_id, Idempotency-Key)` на уровне БД. Повтор запроса возвращает исходный результат.
2. **Transactional outbox.** Уведомление, доставки и сообщения для Kafka сохраняются в одной транзакции (Spring Modulith Event Publication Registry). Принятое уведомление не теряется, даже если Kafka недоступна: незавершённые публикации отправляются повторно.
3. **Идемпотентный воркер.** Доставка в статусе `SENT` и дальше повторно не отправляется. Параллельную обработку одной доставки отсекает оптимистическая блокировка.
4. **Ключ идемпотентности у провайдера.** В него передаётся `deliveryId`: `Idempotency-Key` для вебхуков, `Message-ID` для писем. Получатель может отбросить дубль.
5. **Узкое окно дубля.** Дубль возможен только при падении между ответом провайдера и записью результата. Такие доставки находит планировщик: они зависли в `SENDING` дольше порога.
6. **Дедупликация по шаблону.** Если у шаблона задано окно, одинаковые уведомления одному получателю в этом окне схлопываются.

## Повторы, parking lot и ручной повтор

При временной ошибке доставка переходит в `RETRY_SCHEDULED`, а запись уходит в retry-топик канала с экспоненциальной задержкой. Политика одна для домена и Kafka: `notification.retry.*`. Когда попытки исчерпаны или ошибка постоянная, доставка получает статус `FAILED`, а запись попадает в `notification.delivery.<канал>.v1-dlt` (parking lot, хранение 30 дней).

Разобрать parking lot можно так:

1. Посмотреть записи DLT в Kafka UI. В заголовках `kafka_dlt-exception-*` лежит причина.
2. Устранить причину: исправить адрес, настройки провайдера и т.п.
3. Отправить уведомление заново с новым `Idempotency-Key`. Доставка в статусе `FAILED` не перезапускается: так история попыток сохраняется неизменной.

## Безопасность

OAuth2 Resource Server (JWT). Отправитель берётся из claim `client_id`, затем `azp`, затем `sub`. Отправитель видит только свои уведомления и статистику.

| Scope | Доступ |
|---|---|
| `notifications:send` | Отправка, пакет, отмена |
| `notifications:send-raw` | Готовое содержимое без шаблона (поле `content`) |
| `notifications:read` | Статус, поиск, статистика |
| `templates:manage` | Шаблоны, версии, предпросмотр |
| `preferences:manage` | Предпочтения получателей |

Ошибки возвращаются в формате RFC 9457 Problem Details с полем `errorCode`. При превышении лимита клиента ответ — `429` с `Retry-After`.

Персональные данные в логах маскируются (`a***a@example.com`, `+77***567`). В события Kafka адреса не попадают.
