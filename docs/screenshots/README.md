# Скриншоты админки

Все страницы админки ([frontend.md](../frontend.md)) на английском языке, светлая тема (последний снимок — тёмная). Сняты с `docker compose up --build` после нескольких отправок; переснять — [`frontend/scripts/screenshots.cjs`](../../frontend/scripts/screenshots.cjs).

## Вход и обзор

**Вход** — dev-токен (профиль `local`) с выбором scopes или вставка токена IdP.

![Login](01-login.png)

**Обзор** — счётчики доставок за 7 дней, график, последние уведомления, состояние сервиса.

![Dashboard](02-dashboard.png)

## Уведомления

**Список** — фильтры по получателю, статусу, каналу, периоду; массовая отмена.

![Notifications list](03-notifications-list.png)

**Карточка email-уведомления** — доставки с замаскированным адресом, попытками, ID у провайдера и версией шаблона.

![Email notification](04-notification-email.png)

**SMS через заглушку-логер** — доставка помечена `stub` (`simulated=true`), ID провайдера `stub-sms-…`.

![SMS via logging stub](05-notification-sms-stub.png)

**Отправка** — каналы, контакты, шаблон с подсказками ключей и обязательных переменных, категория, приоритет, `sendAt`, метаданные; тот же запрос как JSON, curl и команда Kafka (CloudEvents).

![Send notification](06-send-notification.png)

**Пакетная отправка** — JSON до 500 элементов или список получателей; результат `207` по каждому элементу.

![Batch send](07-batch-send.png)

## Шаблоны

![Templates list](08-templates-list.png)

**Версии** — статус, обязательные переменные, исходник и HTML-превью; активация и новая версия на основе существующей.

![Template versions](09-template-versions.png)

**Предпросмотр** — рендер выбранной версии с тестовыми переменными, без отправки.

![Template preview](10-template-preview.png)

**Живое превью при создании** — редактор слева, рендер справа с тестовыми значениями переменных. HTML + CSS: кнопка «Insert `<style>`» добавляет заготовку стилей, `th:text` / `th:href` / `[(${x})]` подставляются сразу.

![Create template: HTML + CSS live preview](11-template-create-html-css.png)

**Markdown** (формат определяется автоматически, можно переключить на HTML / MD / Text) в мобильной ширине 375 px.

![Create template: Markdown, mobile width](12-template-create-markdown-mobile.png)

**Редактирование** — новая версия шаблона (версии неизменяемы) с тем же превью.

![New version with live preview](13-template-new-version-preview.png)

## Предпочтения и статистика

**Предпочтения получателя** — отписки, тихие часы, часовой пояс, локаль; сохранение целиком (PUT) и точечная очистка (PATCH).

![Preferences](14-preferences.png)

![Statistics](15-statistics.png)

## Каналы и провайдеры

**Каналы** — включён ли канал, состояние circuit breaker, текущий провайдер (реальный или заглушка-логер), счётчики отправок по исходам.

![Channels and providers](16-channels-and-providers.png)

**Лог заглушки** — последние доставки канала в формате строк, которые пишет логирующий провайдер.

![Stub log](17-stub-log.png)

**Подключение провайдера** — выбор провайдера (встроенный или заготовка), настройки и готовые `.env`, `application.yml`, каркас Java-класса.

![Connect provider: settings](18-connect-provider-settings.png)

![Connect provider: Java skeleton](19-connect-provider-java.png)

**Проверка подписи вебхука** — HMAC-SHA256 `X-Notification-Signature`.

![Webhook signature check](20-webhook-signature.png)

## Система

**Система** — дерево health, текущий токен и scopes, Kafka-топики и типы событий, ссылки на инструменты, настройки фронтенда.

![System](21-system.png)

![Dashboard, dark theme](22-dashboard-dark.png)
