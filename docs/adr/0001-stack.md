# ADR 0001. Технологический стек и версии

- **Статус:** принято
- **Дата:** 2026-09-26

## Контекст

По промпту перед началом работы нужно проверить последние стабильные версии и зафиксировать их. Интернет был доступен только через веб-поиск: Maven Central и Docker Hub из среды разработки закрыты политикой сети. Поэтому версии сверены по официальным страницам и каталогам артефактов, но сборкой в этой среде не проверены. См. `docs/progress.md`.

## Решение

| Компонент | Версия | Как выбрана / источник |
|---|---|---|
| Java | 25 (LTS) | `maven.compiler.release=25` |
| Spring Boot | 4.1.1 | Текущая поддерживаемая ветка (4.1.1 от 20.08.2026; 4.0.x заканчивается 31.12.2026) — spring.io, eosl.date |
| Spring Framework | 7.x | Из BOM Boot 4.1 |
| Spring for Apache Kafka | 4.x | Из BOM Boot 4.1; стартер — `spring-boot-starter-kafka` (новое имя в Boot 4) |
| Apache Kafka (брокер) | 4.1.0, KRaft | Образ `apache/kafka:4.1.0` в docker-compose и Testcontainers |
| Spring Modulith | 2.1.0 | 2.1 GA от 11.06.2026 — линия для Boot 4.1; `spring-modulith-starter-jdbc` + `spring-modulith-events-kafka` |
| Liquibase | из BOM Boot 4.1 | Стартер `spring-boot-starter-liquibase` (Boot 4) |
| PostgreSQL | 17 | `postgres:17-alpine` |
| Resilience4j | 2.4.0 | `resilience4j-spring-boot4` (15.03.2026) — mvnrepository |
| springdoc-openapi | 3.0.3 | Линия 3.x для Boot 4, OpenAPI 3.1 — springdoc.org |
| Thymeleaf | из BOM Boot 4.1 | Используется `thymeleaf-spring6` напрямую, без автоконфигурации view-слоя |
| Jackson | 3.x | В Boot 4 по умолчанию: пакет `tools.jackson`, бин `JsonMapper` |
| Testcontainers | 2.x (из BOM Boot) | Артефакты `testcontainers-postgresql`, `testcontainers-kafka`, `testcontainers-junit-jupiter` |
| GreenMail | 2.1.5 | libraries.io |
| WireMock | 3.13.1 | `org.wiremock:wiremock-standalone` |
| ArchUnit | 1.4.1 | archunit.org |
| JaCoCo | 0.8.14 | Первая версия с официальной поддержкой Java 25 |
| Maven | 3.9.11 | Maven Wrapper (only-script) |

## Зависимости, которых не было в таблице стека промпта

- **JaCoCo** — нужен для требования «покрытие домена и application ≥ 90%».
- **Awaitility** — ожидание асинхронных результатов в интеграционных тестах (из BOM Boot).
- **micrometer-tracing-bridge-otel, micrometer-registry-prometheus** — трассировка и метрики. Требование «наблюдаемость» без конкретного инструмента.
- **ShedLock и Bucket4j не добавлены.** Защиту планировщика даёт `SELECT … FOR UPDATE SKIP LOCKED`, лимит клиента — Resilience4j RateLimiter (см. ADR 0008).

## Последствия

- Версии вне BOM Boot (Modulith, springdoc, Resilience4j, тестовые библиотеки) вынесены в `<properties>` корневого `pom.xml`.
- При первом прогоне `./mvnw verify` на машине с доступом к Maven Central нужно подтвердить, что все артефакты разрешаются. В особенности пакеты API, отмеченные в `docs/progress.md`.
