# Notification Service

Готовый сервис уведомлений для бэкенда. Он принимает команду «уведомить пользователя» через REST или Kafka, формирует текст по шаблону из БД и доставляет его по каналам: **email, SMS, Telegram, WhatsApp, push, webhook**. Статус каждой доставки отслеживается и публикуется событиями в Kafka.

Репозиторий задуман как пример: разверните его как отдельный микросервис или возьмите за основу и подключите своих провайдеров, каналы и правила.

## Возможности

- **Два входа, одна логика:** REST API и Kafka-команды в формате CloudEvents.
- **Шаблоны в БД:** версии, активация, предпросмотр, локали с fallback (`ru-KZ → ru → default`), Thymeleaf.
- **Надёжная доставка:** transactional outbox, at-least-once, повторы с экспоненциальной задержкой через retry-топики, dead letter topics, самовосстановление после сбоев.
- **Идемпотентность:** `Idempotency-Key` на приёме, `deliveryId` как ключ идемпотентности у провайдера, дедупликация по шаблону.
- **Предпочтения получателя:** отписки по каналам и категориям, тихие часы с часовым поясом, лимиты частоты.
- **Отложенная отправка** (`sendAt`), отмена, пакеты до 500 уведомлений, приоритетная полоса `HIGH`.
- **Эксплуатация:** OAuth2 scopes, лимит запросов клиента, circuit breaker на канал, метрики Prometheus, трассировка OpenTelemetry, JSON-логи с маскированием персональных данных.

Email (SMTP) и webhook работают по-настоящему. SMS, Telegram, WhatsApp и push — заглушки, которые пишут в лог и возвращают `simulated=true`; реальный провайдер подключается одним классом ([как](docs/extending.md#1-подключить-реального-провайдера-к-существующему-каналу)).

## Документация

| Документ | Для чего |
|---|---|
| [docs/integration.md](docs/integration.md) | **Подключить к своему бэкенду:** REST API, Kafka-команды и события, вебхуки, ошибки, авторизация |
| [docs/configuration.md](docs/configuration.md) | Все настройки, переменные окружения, топики Kafka, БД, наблюдаемость |
| [docs/architecture.md](docs/architecture.md) | Как устроен: модули, путь уведомления, статусы, гарантии доставки |
| [docs/extending.md](docs/extending.md) | Точки расширения: провайдеры, новые каналы, контакты, шаблоны, политики |
| [docs/how-to-add-channel.md](docs/how-to-add-channel.md) | Пошаговые примеры: Twilio вместо заглушки, канал Viber |
| [docs/asyncapi.yaml](docs/asyncapi.yaml) | Контракты Kafka (AsyncAPI 3.0) |
| [docs/glossary.md](docs/glossary.md) | Термины |
| [docs/adr/](docs/adr) | Архитектурные решения и их причины |
| `/swagger-ui.html` | OpenAPI 3.1 запущенного сервиса (в `prod` выключен) |

## Быстрый старт

Нужен только Docker (Compose v2).

```bash
docker compose up --build
```

Поднимутся PostgreSQL, Kafka (KRaft), Kafka UI, Mailpit и сам сервис с профилем `local`.

| Что | Адрес |
|---|---|
| API и Swagger UI | http://localhost:8080/swagger-ui.html |
| Mailpit (входящие письма) | http://localhost:8025 |
| Kafka UI | http://localhost:8088 |
| Kafka с хоста | `localhost:29092` |
| Health | http://localhost:8080/actuator/health |

**1. Получить токен** (только профиль `local`):

```bash
TOKEN=$(curl -s -X POST localhost:8080/dev/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"orders-service"}' | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
```

**2. Отправить письмо:**

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

Ответ — `202 Accepted` с `Location: /api/v1/notifications/{id}`. Письмо «Добро пожаловать, Анна!» появится в Mailpit. Повтор с тем же `Idempotency-Key` вернёт тот же `id` и заголовок `Idempotent-Replayed: true`.

**3. Посмотреть статус:**

```bash
curl -s localhost:8080/api/v1/notifications/<id> -H "Authorization: Bearer $TOKEN"
```

**4. Несколько каналов и отложенная отправка:**

```bash
# SMS и Telegram — заглушки: в ответе simulated=true
curl -s -X POST localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-2' -H 'Content-Type: application/json' \
  -d '{"recipient":{"id":"user-42","contacts":{"phone":"+77011234567","telegramChatId":"123456"}},
       "template":{"key":"order-shipped","variables":{"orderId":"ORD-42"}},
       "channels":["SMS","TELEGRAM"],"category":"TRANSACTIONAL","priority":"HIGH"}'

# Отложенная отправка; отменить можно до её начала: POST /api/v1/notifications/<id>/cancel
curl -s -X POST localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-3' -H 'Content-Type: application/json' \
  -d '{"recipient":{"id":"user-42","contacts":{"email":"anna@example.com"}},
       "template":{"key":"welcome","variables":{"name":"Анна"}},
       "channels":["EMAIL"],"category":"MARKETING","sendAt":"2030-01-01T09:00:00+05:00"}'
```

**5. Через Kafka:**

```bash
docker compose exec kafka bash -c '
cat <<EOF | /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic notification.send.requests.v1 \
  --property parse.key=true --property parse.headers=true \
  --property headers.delimiter="|" --property key.separator=";"
ce_id:evt-1,ce_source:urn:local:demo-producer,ce_type:kz.company.notification.send.v1,ce_specversion:1.0,content-type:application/json|user-42;{"recipient":{"id":"user-42","contacts":{"email":"anna@example.com"}},"template":{"key":"order-shipped","variables":{"orderId":"ORD-77"}},"channels":["EMAIL"],"category":"TRANSACTIONAL"}
EOF'
```

События статусов появятся в `notification.events.v1` (видно в Kafka UI). Подробности контрактов — [docs/integration.md](docs/integration.md).

## Email

Email — основной рабочий канал: multipart (текст + HTML) в UTF-8, `Message-ID: <deliveryId@домен>`, `List-Unsubscribe` для marketing-писем. Настройки задаются только переменными окружения, например `.env` для `docker compose --env-file .env up`:

```dotenv
SMTP_HOST=smtp.company.kz
SMTP_PORT=587
SMTP_USERNAME=notifications@company.kz
SMTP_PASSWORD=change-me
SMTP_AUTH=true
SMTP_STARTTLS=true        # 587 + STARTTLS
SMTP_SSL=false            # true для 465 (SMTPS), тогда SMTP_STARTTLS=false
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
| Microsoft 365 | smtp.office365.com:587, STARTTLS | Basic auth для SMTP отключается Microsoft: нужен OAuth2 (XOAUTH2) — своя реализация `EmailProvider` |
| Google Workspace | smtp.gmail.com:587 STARTTLS или 465 SSL | Пароль приложения или SMTP relay (smtp-relay.gmail.com) |

SMTP `4xx`, сетевые ошибки и ошибки аутентификации повторяются; `5xx` (нет ящика, отказ в релее) — сразу `FAILED`. Индикатор `mail` в `/actuator/health` проверяет соединение с сервером.

Остальные переменные окружения и все настройки — [docs/configuration.md](docs/configuration.md).

## Сборка и тесты

Нужны **JDK 25** и Docker (интеграционные тесты поднимают PostgreSQL и Kafka в Testcontainers).

```bash
./mvnw verify
```

| Уровень | Где | Что проверяется |
|---|---|---|
| Домен | `notification-domain/src/test` | инварианты, машина состояний, политики; без Spring |
| Application | `notification-application/src/test` | сценарии на in-memory фейках портов |
| Адаптеры | `notification-infrastructure/src/test` | SMTP (GreenMail), webhook (WireMock), Thymeleaf, маппинг, Kafka-воркер |
| REST-контракт | `notification-presentation/.../web` | `@WebMvcTest`: коды, заголовки, Problem Details, scopes |
| Интеграция | `notification-presentation/.../integration/*IT` | REST/Kafka → БД → Kafka → канал → событие, повторы и DLT, идемпотентность, отказ брокера |
| Архитектура | `notification-presentation/.../architecture` | ArchUnit: направление зависимостей, имена, внедрение через конструктор |

Образ для деплоя собирает `Dockerfile` (JRE 25, непривилегированный пользователь, слои Spring Boot).

## Структура

```
notification-domain/          домен: агрегаты, политики, события — чистая Java
notification-application/     сценарии и порты (интерфейсы для адаптеров)
notification-infrastructure/  адаптеры: REST, Kafka, JPA, каналы, шаблоны
notification-presentation/    Spring Boot приложение, конфигурация, Liquibase
docs/                         документация для разработчиков
ai/                           рабочие заметки ИИ-агента (людям не нужны)
```

**Стек:** Java 25, Spring Boot 4.1, Spring for Apache Kafka 4, Spring Modulith 2.1, PostgreSQL 17, Liquibase, Apache Kafka 4 (KRaft), Thymeleaf 3.1, Resilience4j, Micrometer + OpenTelemetry.
