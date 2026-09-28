# ADR 0002. Луковая архитектура на модулях Maven

- **Статус:** принято

## Решение

Четыре модуля, зависимости направлены строго внутрь:

```
notification-bootstrap ──► notification-infrastructure ──► notification-application ──► notification-domain
         (всё)                    (адаптеры)                   (use cases, порты)            (чистая Java)
```

- `domain` — только JDK. Агрегаты `Notification`, `Delivery`, `Template`, `RecipientPreferences`, value objects, события, политики и интерфейсы репозиториев.
- `application` — входящие порты (use cases), исходящие порты и их реализации-сценарии. Из Spring разрешён только `spring-tx` (ADR 0003).
- `infrastructure` — адаптеры: `in.rest`, `in.messaging`, `in.events`, `in.scheduling`, `out.persistence`, `out.messaging`, `out.channel.*`, `out.template`, `out.recipient`.
- `bootstrap` — `@SpringBootApplication`, `@Configuration`, сборка сценариев в бины, `application.yml`, Liquibase.

Нарушение направления не компилируется: у модуля просто нет зависимости. ArchUnit (`ArchitectureTest`) дополнительно проверяет:

- в домене нет Spring;
- у application нет ничего, кроме `spring-tx`/`spring-dao`;
- входящие адаптеры не используют исходящие и наоборот;
- соглашения об именах: `*Controller`, `*JpaEntity`, `*ChannelSender`;
- нет внедрения через поля.

## Последствия

- Классы сценариев без `@Service`: бины создаются в `UseCaseConfiguration`. Сценарии тестируются без Spring на in-memory фейках.
- JPA-сущности отделены от доменных объектов. Маппинг идёт через снимки (`*Snapshot`) и фабрики `restore(...)`, поэтому у доменных объектов нет публичных сеттеров.
