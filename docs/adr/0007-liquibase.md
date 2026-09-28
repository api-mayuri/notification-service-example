# ADR 0007. Миграции Liquibase

- **Статус:** принято (по требованию заказчика вместо Flyway)

## Решение

- Master changelog: `notification-bootstrap/src/main/resources/db/changelog/db.changelog-master.yaml`. Изменения лежат в `changes/` в формате formatted SQL (`--changeset`, `--rollback`).
- Одно изменение — один changeset. Применённые changeset'ы не редактируются, только добавляются новые.
- У каждого изменения схемы есть `rollback`.
- Начальные шаблоны (welcome, password-reset, email-verification, order-shipped) — отдельный changeset `100-seed-templates.sql` с label `seed`. Данные отделены от схемы, их можно исключить через `spring.liquibase.label-filter`.
- Таблица `event_publication` Spring Modulith создаётся changeset'ом `005`, автоинициализация Modulith выключена.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate только сверяет схему.
