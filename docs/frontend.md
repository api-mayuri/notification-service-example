# Админка (frontend)

Веб-интерфейс сервиса на [react-admin](https://github.com/marmelab/react-admin) 5: React 19, MUI 9, Vite 8 (Rolldown), TypeScript 7, Vitest 5; Node 22.12+ (образ — Node 24 LTS). Код — в [`frontend/`](../frontend). Интерфейс на русском и английском, светлая и тёмная тема. Как выглядят страницы — [screenshots/](screenshots/README.md).

## Запуск

**Вместе со всем стеком** (нужен только Docker):

```bash
docker compose up --build
```

Админка — http://localhost:3000. На странице входа вкладка «Dev-токен» выдаёт тестовый токен через `POST /dev/token` (профиль `local`); clientId по умолчанию `admin-ui`. Клиент видит только свои уведомления: чтобы увидеть отправленные из curl с `orders-service`, войдите с тем же clientId.

**Для разработки фронтенда:**

```bash
docker compose up --build notification-service   # бэкенд и его зависимости
cd frontend
npm ci
npm run dev                                       # http://localhost:5173
```

Vite проксирует `/api`, `/dev`, `/actuator`, `/v3/api-docs`, `/swagger-ui` на `DEV_BACKEND_URL` (по умолчанию `http://localhost:8080`), поэтому CORS на бэкенде не нужен.

| Команда | Что делает |
|---|---|
| `npm run dev` | dev-сервер с hot reload |
| `npm test` | unit-тесты (vitest): data provider, разбор ошибок, сборка запросов, реестр провайдеров, полнота переводов |
| `npm run build` | проверка типов и сборка в `dist/` |
| `node scripts/screenshots.cjs ../docs/screenshots` | переснять [скриншоты](screenshots/README.md) с запущенного стека (нужен Playwright, см. комментарий в скрипте) |

## Что покрыто

| Раздел | API | Возможности |
|---|---|---|
| Уведомления | `GET /api/v1/notifications`, `GET /{id}` | поиск по получателю, статусу, каналу и периоду; карточка с доставками (статус, попытки, замаскированный адрес, ошибка, причина пропуска, `simulated`, версия шаблона); автообновление, пока уведомление не завершено |
| Отправка | `POST /api/v1/notifications` | шаблон или готовый текст (`send-raw`), контакты по каналам и свои типы контактов, переменные (подсказки ключей и обязательных переменных из шаблонов), категория, приоритет, `sendAt`, метаданные, `Idempotency-Key`; показ итога `CREATED` / `REPLAYED` / `DEDUPLICATED`; тот же запрос как JSON, curl и команда Kafka (CloudEvents) |
| Отмена | `POST /{id}/cancel` | из карточки и массово из списка |
| Пакет | `POST /api/v1/notifications/batch` | JSON до 500 элементов или список получателей «id,контакт,переменные» для одного шаблона; таблица результата `207` по элементам |
| Шаблоны | `GET/POST /api/v1/templates`, `GET/PUT /{key}`, `POST /{key}/versions`, `/activate`, `/preview` | список с фильтрами, создание, версии с HTML-превью, активация, новая версия (в т.ч. на основе существующей), окно дедупликации, предпросмотр с тестовыми переменными, варианты ключа по каналам и локалям |
| Предпочтения | `GET/PUT/PATCH /api/v1/recipients/{id}/preferences` | отписки от каналов и категорий, тихие часы, часовой пояс, локаль; PUT — сохранить целиком, PATCH — убрать тихие часы или локаль |
| Статистика | `GET /api/v1/statistics` | период до 92 дней, канал, шаг `HOUR`/`DAY`; график по статусам и итоги по каналам |
| Каналы и провайдеры | `/actuator/metrics/notification.channel.send`, `/actuator/metrics/resilience4j.circuitbreaker.*` | см. ниже |
| Система | `/actuator/health`, `/actuator/info` | дерево health, токен и его scopes, Kafka-топики и типы событий, ссылки на Swagger UI, Mailpit, Kafka UI, Prometheus, текущие настройки фронтенда |

Пункты меню показываются по scopes токена (`notifications:read`, `notifications:send`, `templates:manage`, `preferences:manage`). Ошибки API (Problem Details) выводятся как `errorCode: detail`; ошибки полей (`VALIDATION_FAILED`) подсвечивают поля формы. `401` возвращает на страницу входа, `403` — только сообщение.

## Каналы и провайдеры

Страница «Каналы и провайдеры» показывает по каждому каналу:

- включён ли канал и состояние его circuit breaker `channel-<канал>` (метрики `resilience4j.circuitbreaker.state` и `failure.rate`; breaker есть только у включённых каналов);
- текущего провайдера (`smtp`, `http`, `logging`) и его тип: реальный или **заглушка-логер**;
- счётчики отправок с момента запуска по исходам (`success`, `simulated`, `retryable_failure`, `permanent_failure`, `error`, `circuit_open`) и среднее время;
- для заглушек — «Лог заглушки»: последние доставки канала в формате строк, которые пишет `StubProviderSupport`, и команду для настоящего лога (`docker compose logs -f notification-service | grep "Stub SMS provider"`);
- для webhook — проверку подписи `X-Notification-Signature` (HMAC-SHA256);
- кнопку «Тест» — форма отправки с выбранным каналом.

**Подключение провайдера** — мастер из трёх шагов: выбрать провайдера, заполнить настройки, получить `.env`, фрагмент `application.yml` и (для ещё не реализованных) каркас Java-класса по [extending.md](extending.md#1-подключить-реального-провайдера-к-существующему-каналу). На сервере мастер ничего не сохраняет: настройки бэкенда задаются только переменными окружения.

Провайдеры описаны в реестре фронтенда:

| Файл | Что внутри |
|---|---|
| `src/channels/providers/builtIn.ts` | реализованные на бэкенде: `smtp`, `http`, `logging` для каждого канала (с настройками `stub.*` для случайных сбоев) |
| `src/channels/providers/planned.ts` | заготовки: Twilio и Mobizon (SMS), Telegram Bot API, WhatsApp Cloud API, FCM и APNs (push), SendGrid (email) |
| `src/channels/registry.ts` | порты провайдеров по каналам, генерация `.env` / YAML / Java |

Как добавить провайдера в интерфейс: опишите его в `planned.ts` (id — значение `notification.channels.<канал>.provider`, настройки — переменные окружения и свойства). Когда класс появится на бэкенде, перенесите описание в `builtIn.ts` с `availability: 'built-in'`. Новый канал (например, VIBER) добавьте в `FRONTEND_EXTRA_CHANNELS`.

Какой провайдер выбран, фронтенд узнаёт из настройки `CHANNEL_PROVIDERS`. В `docker-compose.yml` она собирается из тех же переменных `<КАНАЛ>_PROVIDER`, что передаются сервису, поэтому одна строка в `.env` (`SMS_PROVIDER=twilio`) меняет и бэкенд, и отображение.

## Настройки

Настройки задаются переменными окружения и применяются при старте контейнера: скрипт `docker/40-app-config.sh` пишет их в `/config.js`, пересобирать образ не нужно. В `docker compose` они берутся из `.env` рядом с `docker-compose.yml` (пример — [`.env.example`](../.env.example)).

| Переменная compose | Переменная контейнера | По умолчанию | Описание |
|---|---|---|---|
| `FRONTEND_PORT` | — | `3000` | порт админки на хосте |
| `FRONTEND_TITLE` | `APP_TITLE` | `Notification Service` | заголовок |
| `FRONTEND_AUTH_MODE` | `AUTH_MODE` | `dev-token` | `dev-token` — вход через `/dev/token` и вставка токена; `bearer` — только вставка токена IdP (для `prod`) |
| `FRONTEND_DEV_CLIENT_ID` | `DEV_TOKEN_CLIENT_ID` | `admin-ui` | clientId по умолчанию на странице входа |
| `FRONTEND_LOCALE` | `DEFAULT_LOCALE` | `ru` | `ru` или `en` |
| `FRONTEND_POLL_INTERVAL_MS` | `POLL_INTERVAL_MS` | `5000` | автообновление карточки уведомления; `0` — выключить |
| `FRONTEND_SWAGGER_URL` | `SWAGGER_URL` | `/swagger-ui.html` | ссылка на Swagger UI |
| `FRONTEND_MAILPIT_URL` | `MAILPIT_URL` | `http://localhost:8025` | ссылка на Mailpit |
| `FRONTEND_KAFKA_UI_URL` | `KAFKA_UI_URL` | `http://localhost:8088` | ссылка на Kafka UI |
| `FRONTEND_EXTRA_CHANNELS` | `EXTRA_CHANNELS` | — | каналы, добавленные на бэкенде, через запятую |
| `<КАНАЛ>_PROVIDER` | `CHANNEL_PROVIDERS` | `EMAIL=smtp,SMS=logging,…` | провайдер по каналам (см. выше) |
| — | `API_URL` | пусто | адрес API; пусто — тот же origin через nginx. Другой origin потребует CORS на бэкенде |
| — | `BACKEND_URL` | `http://notification-service:8080` | куда nginx проксирует API |
| — | `NGINX_RESOLVER` | `127.0.0.11` | DNS для `BACKEND_URL` (Docker DNS; в Kubernetes — адрес kube-dns) |

Для `npm run dev` / `npm run build` те же настройки задаются в `frontend/.env` с префиксом `VITE_` (пример — [`frontend/.env.example`](../frontend/.env.example)). Значения из `/config.js` важнее `VITE_*`.

## Образ и деплой

`frontend/Dockerfile`: сборка на Node 24 (`npm ci`, тесты, проверка типов, `vite build`), затем nginx 1.29:

- раздаёт статику; неизвестные пути отдают `index.html` (маршруты SPA);
- проксирует `/api`, `/dev`, `/actuator`, `/v3/api-docs`, `/swagger-ui*` на `BACKEND_URL` с заголовками `X-Forwarded-*` (бэкенд их учитывает: `server.forward-headers-strategy: framework`);
- адрес бэкенда разрешается на каждый запрос, поэтому nginx стартует раньше сервиса и переживает его перезапуск;
- `/config.js` не кэшируется, файлы `/assets/*` с хэшем кэшируются навсегда;
- `/healthz` — проверка живости контейнера.

**В `prod`:** поставьте `AUTH_MODE=bearer` — `/dev/token` там выключен. Токен IdP должен содержать `aud` = `JWT_AUDIENCE` сервиса и нужные scopes. Swagger UI в `prod` выключен, ссылку можно убрать. `/actuator` в `prod` слушает отдельный порт 8081, поэтому страницы «Система» и «Каналы» покажут только то, что доступно на основном порту. Если сервис должен видеть входящие запросы как HTTPS, терминируйте TLS перед nginx и передавайте `X-Forwarded-Proto`.

Вход через OIDC (redirect на IdP) пока не реализован: `authProvider` принимает готовый токен (`login({ mode: 'bearer', token })`), поэтому OIDC-клиент подключается своей страницей входа без изменений остального кода.
