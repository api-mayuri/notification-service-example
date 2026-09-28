# ADR 0010. Безопасность API

- **Статус:** принято

- **OAuth2 Resource Server с JWT.** Issuer задаётся в `spring.security.oauth2.resourceserver.jwt.issuer-uri` (профиль `prod`, переменная `JWT_ISSUER_URI`). Scopes проверяются в `SecurityFilterChain`, `notifications:send-raw` — в контроллере при наличии поля `content`.
- **Отправитель.** Берётся из claim `client_id` (RFC 9068), иначе `azp`, иначе `sub`.
- **Локальный запуск.** `notification.security.dev-tokens.enabled=true` включает генерацию RSA-ключа при старте, `JwtDecoder` на нём и `POST /dev/token`. Это нужно только для «запуска за 5 минут» и тестов. В prod свойство выключено, бинов нет.
- **Шаблоны.** Переменные передаются в контекст Thymeleaf как значения и не вычисляются как выражения. В HTML-режиме вывод экранируется. Выражения в шаблонах пишут только владельцы `templates:manage`. Thymeleaf 3.1 дополнительно ограничивает выражения.
- **Вебхуки.** Подписываются HMAC-SHA256: `X-Notification-Signature: t=<unix>,v1=<hex>` от `timestamp + "." + body`. В prod разрешён только HTTPS, редиректы не выполняются.
- **Персональные данные.** В логах адреса маскируются, в событиях Kafka адресов нет. Контакты из запроса хранятся только в доставках (адрес канала).
