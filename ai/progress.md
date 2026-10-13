# Прогресс

- **Сессия:** 1
- **Дата:** 2026-09-26
- **Статус:** этапы 0–10 выполнены в коде и документации. 2026-10-13: `./mvnw verify` на JDK 25 проходит полностью, включая все `*IT` (см. «Исправления после первого прогона»).

## Документация (2026-10-13)

- Документация разделена: для разработчиков — `README.md` и `docs/` (`architecture.md`, `integration.md`, `configuration.md`, `extending.md`); для ИИ — `CLAUDE.md` и `ai/` (этот журнал, `plan.md`, `research.md`).
- Раздел «Что НЕ проверено» ниже исторический: `./mvnw verify` на JDK 25 проходит. Не проверен только `docker compose up` вручную.
- Найдено при описании настроек: `notification.delivery.rate-limits.<канал>` без `categories` не действует ни на одну категорию (JavaDoc `NotificationProperties` обещает MARKETING по умолчанию). Исправлено: без `categories` лимит применяется к MARKETING (`UseCaseConfiguration`, тест `UseCaseConfigurationTest`).

## Безопасность (2026-10-14)

- В `prod` не проверялся `aud`: принимался любой токен того же IdP со scope `notifications:*`. Добавлено `spring.security.oauth2.resourceserver.jwt.audiences: ${JWT_AUDIENCE:notification-service}`; тест `ProdJwtAudienceTest` (фейковый IdP на JDK HttpServer, реальный `application-prod.yml`).
- Проверка деплоя `docker compose up --build`: REST и Kafka сценарии из README проходят, письма в Mailpit, события в `notification.events.v1`. Исправлен healthcheck сервиса в `docker-compose.yml`: `/dev/tcp` запускался через dash (`CMD-SHELL`) и всегда падал, контейнер не становился healthy.

## Исправления после первого прогона (2026-10-13)

- **Доставки не работали.** `spring-modulith-events-kafka` регистрирует JSON `RecordMessageConverter`, и в Kafka уходила вся обёртка `ExternalMessage` вместо payload: воркеры не находили `deliveryId`, всё уходило в DLT, уведомления оставались `ACCEPTED`. Исправлено `spring.modulith.events.kafka.enable-json=false`; `NotificationFlowIT` теперь проверяет тело события.
- **Java 25 обязательна.** На JDK 21 виртуальные потоки блокируют carrier внутри `synchronized` (исправлено в JDK 24, JEP 491): Kafka-клиенты зависали при закрытии контекста. Откат на 21 отменён; `ContainerCustomizer` с отдельным executor удалён (не применялся Boot), фабрика `sendRequestContainerFactory` собирается через Boot-конфигуратор.
- **Потерянные сообщения доставок.** Планировщик теперь возвращает в очередь и `PENDING`, поставленные в топик раньше `overdue-retry-grace` (`lockOverdueDispatches`, changeset 006).
- **Гонка статуса уведомления.** `NotificationStatusListener` повторяет пересчёт при `OptimisticLockingFailureException`.
- **SMTP.** Порт или таймаут в тексте сетевой ошибки больше не принимается за код 5xx.
- **Concurrency слушателей** ограничена числом партиций.

## Отступления от процесса промпта

- **Этап 0 без остановки.** Заказчик попросил собрать сервис целиком. Противоречия промпта разобраны до начала работы и решены рекомендованными вариантами (ADR 0004, 0005, 0008, 0009).
- **Liquibase вместо Flyway** — по требованию заказчика (ADR 0007).

## Что сделано

| Этап | Результат |
|---|---|
| 0. Анализ и план | `research.md`, `glossary.md`, `plan.md`, ADR 0001–0011 |
| 1. Каркас | Многомодульный Maven, Maven Wrapper (only-script), Dockerfile, docker-compose (PostgreSQL, Kafka KRaft, Kafka UI, Mailpit), ArchUnit, Liquibase |
| 2. Домен | 4 агрегата, value objects, события, политики. 88 тестов |
| 3. Application | 14 входящих портов, 9 исходящих, сценарии. 47 тестов на in-memory фейках |
| 4. Хранение | JPA-сущности, мапперы через снимки, `FOR UPDATE SKIP LOCKED`, Specification, статистика, changelog |
| 5. REST | Контроллеры, Idempotency-Key, Problem Details, OAuth2 scopes, rate limit 429, OpenAPI 3.1 |
| 6. Брокер | Входящий слушатель CloudEvents, outbox (Modulith), топики по каналам и полосам, retry-топики, DLT, события статусов, AsyncAPI |
| 7. Каналы и шаблоны | SMTP и webhook (HMAC) — рабочие; SMS, Telegram, WhatsApp, push — порты и заглушки с `simulated`; Thymeleaf из БД с кэшем; предпросмотр |
| 8. Предпочтения и расписание | Отписки, тихие часы, `sendAt`, лимиты частоты (откладывание), дедупликация, статистика |
| 9. Наблюдаемость | Метрики каналов, circuit breaker, трассировка (OTel bridge, Kafka observation), health (mail, circuit breakers), JSON-логи ECS, маскирование ПДн |
| 10. Сдача | `notification-service.zip` |

## Что проверено в этой среде

Ограничения среды:

- Maven Central, Docker Hub и GitHub API закрыты политикой сети.
- Docker daemon не запущен.
- Установлен только JDK 21.

Что удалось проверить:

- **Домен и application:**
  - скомпилированы `javac` (синтаксис совместим с Java 21, целевой релиз — 25);
  - все 88 + 47 тестов прогнаны против минимальных заглушек API JUnit/AssertJ — все зелёные;
  - покрытие строк измерено через JDI: домен 97,1%, application 96,9% (порог JaCoCo — 90%).
- **Maven Wrapper:** проверен с локальным Maven 3.9.11.
- **Статические проверки:** YAML (`application*.yml`, `docker-compose.yml`, `asyncapi.yaml`, changelog) и все `pom.xml` синтаксически валидны; SQL seed проверен на баланс кавычек.
- **API Spring Modulith** (`EventExternalizationConfiguration.Router`, `RoutingTarget.forTarget().andKey()`) и **Resilience Features Spring Framework 7** (`@Retryable`, `@ConcurrencyLimit(limitString)`) сверены с актуальной документацией.

## Что НЕ проверено (сделать первым в следующей сессии)

1. **`./mvnw verify` на машине с JDK 25, Docker и доступом к Maven Central.** Модули infrastructure и bootstrap здесь не компилировались. Места, где API написан по памяти и может потребовать правки:
   - **Spring Boot 4 (тестовые срезы):** пакеты `org.springframework.boot.webmvc.test.autoconfigure.{WebMvcTest, AutoConfigureMockMvc}`, `org.springframework.boot.persistence.autoconfigure.EntityScan`.
   - **Testcontainers 2:** `org.testcontainers.postgresql.PostgreSQLContainer` (в 1.x — `org.testcontainers.containers`).
   - **Spring for Apache Kafka 4:** `RetryTopicConfigurationBuilder` (`notRetryOn(List)`, `dltHandlerMethod`, `autoCreateTopicsWith`), `ExponentialBackOffWithMaxRetries`.
   - **Jackson 3:** `JsonNode.asString()`, `JacksonException.getOriginalMessage()`.
   - **Thymeleaf 3.1:** сигнатуры `AbstractTemplateResolver.compute*`, `setCheckExistence`.
   - **Spring Framework 7:** формат `@Retryable(delayString = "200")` (миллисекунды числом).
   - **Совместимость версий:** springdoc 3.0.3 с Boot 4.1; `resilience4j-spring-boot4` 2.4.0 с Boot 4.1.
   - **Spring Kafka `JsonKafkaHeaderMapper`:** как пишутся строковые заголовки CloudEvents. Ожидаются сырые байты; наши читатели на всякий случай снимают кавычки.
2. **`docker compose up --build`:** образы `maven:3.9-eclipse-temurin-25`, `eclipse-temurin:25-jre`, `apache/kafka:4.1.0`, `ghcr.io/kafbat/kafka-ui:latest`, `axllent/mailpit:latest`. Затем пройти сценарий из README и убедиться, что письмо появилось в Mailpit.
3. **Интеграционные тесты `*IT`.** Особенно чувствительны ко времени:
   - `OutboxResilienceIT` — пауза контейнера Kafka;
   - `DeliveryFailureIT` — retry-топики с задержками 1–2 с.
   При нестабильности увеличить таймауты Awaitility.

## Открытые вопросы к заказчику

- Нужен ли эндпоинт подтверждений доставки от провайдеров (`SENT → DELIVERED`)? Домен готов, формат callback'ов зависит от выбранных провайдеров.
- Распределённый лимит запросов клиента (Redis/Bucket4j) — нужна новая зависимость (ADR 0008).
- Должен ли отправитель со специальным scope видеть уведомления других клиентов (админ-доступ)? Сейчас каждый видит только свои.
- OAuth2 (XOAUTH2) для Microsoft 365 / Google Workspace — отдельная реализация `EmailProvider`, когда будет выбран вариант.

## Следующий шаг

Прогнать `./mvnw verify`, исправить расхождения API из списка выше, затем `docker compose up` и сценарий из README.
