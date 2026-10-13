# Как расширять и менять поведение

Сервис — пример, который берут за основу: форкают репозиторий, подключают своих провайдеров, каналы и правила. Расширение строится на портах (интерфейсах) из `notification-application/.../port/out`: домен и сценарии не меняются, меняются и добавляются адаптеры в `notification-infrastructure`.

## Карта точек расширения

| Что хотите сделать | Что реализовать или поменять | Где |
|---|---|---|
| Подключить реального SMS / Telegram / WhatsApp / push-провайдера | `SmsProvider`, `TelegramProvider`, `WhatsAppProvider`, `PushProvider` | `infrastructure/out/channel/<канал>/` |
| Другой способ отправки email (OAuth2, SES, SendGrid) | `EmailProvider` | `out/channel/email/` |
| Свой формат вебхука | `WebhookProvider` | `out/channel/webhook/` |
| Новый канал (Viber, Slack, in-app…) | `ChannelSender` (+ свой `*Provider` по желанию) | `out/channel/<канал>/` |
| Брать контакты из своего сервиса пользователей | `RecipientResolver` или просто `RECIPIENT_DIRECTORY_URL` | `out/recipient/` |
| Другой движок шаблонов | `TemplateRenderer` | `out/template/` |
| Свои идентификаторы | `IdGenerator` | `out/system/` |
| Другой брокер вместо Kafka | `DeliveryDispatcher`, `DomainEventPublisher` + входящие адаптеры | `out/messaging/`, `in/messaging/` |
| Политика повторов | `notification.retry.*` или бин `RetryPolicy` | `bootstrap/config/UseCaseConfiguration` |
| Лимиты частоты | `notification.delivery.rate-limits.*` | `application.yml` |
| Тихие часы, выбор каналов, расчёт статуса | `QuietHoursPolicy`, `ChannelSelectionPolicy`, `NotificationStatusCalculator` | `domain/policy/` |
| Свои события наружу | `StatusEventMapper` | `out/messaging/` |
| Откуда брать `clientId`, scopes, правила доступа | `AuthenticatedClient`, `SecurityConfiguration` | `in/rest/`, `bootstrap/config/` |

## 1. Подключить реального провайдера к существующему каналу

У каждого канала есть интерфейс провайдера, а реализация выбирается свойством `notification.channels.<канал>.provider` через `@ConditionalOnProperty`. Из коробки SMS, Telegram, WhatsApp и push работают через логирующие заглушки (`provider: logging`).

```java
@Component
@ConditionalOnProperty(prefix = "notification.channels.sms", name = "provider", havingValue = "mobizon")
public class MobizonSmsProvider implements SmsProvider {

    @Override
    public SendResult send(SmsMessage message) {
        // message.to() — проверенный номер, message.text() — отрендеренный текст,
        // message.deliveryId() — передайте провайдеру как ключ идемпотентности
        try {
            ProviderResponse response = client.send(message);
            return SendResult.Success.real(response.messageId());
        } catch (ProviderTemporaryException e) {
            return new SendResult.RetryableFailure(e.getMessage());   // сеть, 429, 5xx → повтор
        } catch (ProviderRejectedException e) {
            return new SendResult.PermanentFailure(e.getMessage());   // неверный номер → сразу FAILED
        }
    }
}
```

```yaml
notification:
  channels:
    sms:
      provider: mobizon
```

**Контракт провайдера** (JavaDoc каждого интерфейса):

- исключения наружу не бросать: непредвиденное исключение считается временной ошибкой;
- временная ошибка → `RetryableFailure` (повтор по `notification.retry.*`, считается сбоем для circuit breaker);
- постоянная ошибка → `PermanentFailure` (сразу `FAILED` и DLT; circuit breaker её не считает);
- метод может быть вызван повторно для той же доставки: передавайте `deliveryId` провайдеру как ключ идемпотентности, если он это поддерживает.

Circuit breaker, метрики, лимит параллелизма и повторы уже обёрнуты вокруг канала — в провайдере их делать не нужно. Подробный пример с Twilio и особенности ошибок Telegram и FCM — [how-to-add-channel.md](how-to-add-channel.md#a-заменить-заглушку-реальным-провайдером-пример-sms-через-twilio).

## 2. Добавить новый канал

`ChannelType` и `ContactType` — открытые типы (record), а не enum: новый канал не требует правок домена.

```java
@Component
@ConditionalOnProperty(prefix = "notification.channels.viber", name = "enabled", havingValue = "true")
public class ViberChannelSender implements ChannelSender {

    static final ChannelType VIBER = new ChannelType("VIBER");
    static final ContactType VIBER_ID = new ContactType("viberId");

    @Override public ChannelType channel() { return VIBER; }
    @Override public ContactType contactType() { return VIBER_ID; }
    @Override public ChannelAddress parseAddress(String raw) { return new ViberId(raw); }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.viber.max-concurrency:10}")
    public SendResult send(Delivery delivery) {
        ViberId to = (ViberId) delivery.address().orElseThrow();
        RenderedMessage message = delivery.message().orElseThrow();
        // вызвать провайдера, вернуть Success / RetryableFailure / PermanentFailure
    }
}
```

```yaml
notification:
  channels:
    viber:
      enabled: true
      max-concurrency: 10
      concurrency: 3
      high-priority-concurrency: 3
```

Что подключится само:

- `SpringChannelSenderRegistry` найдёт бин и обернёт его circuit breaker'ом `channel-viber` и метриками;
- `DeliveryListenerRegistrar` создаст воркеры для `notification.delivery.viber.v1` и `.high.v1`;
- `KafkaConfiguration` создаст топики, retry-топики и DLT;
- API начнёт принимать `"channels": ["VIBER"]` и контакт `recipient.contacts.viberId`.

Останется завести шаблоны для канала (`POST /api/v1/templates` с `"channel": "VIBER"`). Для каналов кроме EMAIL тело шаблона рендерится в режиме TEXT и попадает в `RenderedMessage.plainText()`.

Требования ArchUnit: класс называется `*ChannelSender` и лежит в `..infrastructure.out.channel..`; внедрение только через конструктор.

## 3. Контакты получателя

Порт `RecipientResolver` вызывается, только если в запросе не хватает контакта для запрошенного канала.

- **Без кода:** задайте `RECIPIENT_DIRECTORY_URL` — включится `HttpRecipientResolver`, формат ответа описан в [integration.md](integration.md#сервис-пользователей-необязательно).
- **Свой источник** (БД, gRPC, LDAP): реализуйте интерфейс. Встроенные реализации выбираются по `notification.recipient-directory.base-url`, поэтому свою отметьте `@Primary` или выключите их условием.

```java
@Component
@Primary
public class GrpcRecipientResolver implements RecipientResolver {
    @Override
    public RecipientContacts resolve(RecipientId recipientId) {
        UserContacts user = usersStub.getContacts(recipientId.value());   // недоступность → исключение
        return new RecipientContacts(Map.of(ContactType.EMAIL, user.getEmail(), ContactType.PHONE, user.getPhone()));
    }
}
```

Если справочник недоступен, бросайте исключение: запрос будет отклонён целиком, и клиент повторит его с тем же ключом. Если получатель не найден, верните `RecipientContacts.NONE`.

## Шаблоны

Шаблоны хранятся в БД и управляются через API (scope `templates:manage`):

| Запрос | Что делает |
|---|---|
| `POST /api/v1/templates` | создать шаблон `key + channel + locale` с первой версией (`activate: true` — сразу активна) |
| `POST /api/v1/templates/{key}/versions` | добавить версию |
| `POST /api/v1/templates/{key}/activate` | активировать версию `{channel, locale, version}` |
| `PUT /api/v1/templates/{key}` | окно дедупликации (`deduplicationWindow`, ISO 8601, `null` — выключить) |
| `POST /api/v1/templates/{key}/preview` | отрендерить с переменными, ничего не отправляя |
| `GET /api/v1/templates`, `/{key}` | список и версии |

Синтаксис — Thymeleaf 3.1. Тема и текстовые тела — режим TEXT (`[(${name})]`), HTML-тело письма — режим HTML (`<span th:text="${name}">`). `requiredVariables` проверяются при приёме: если переменной нет, запрос отклоняется с `422 MISSING_TEMPLATE_VARIABLES`. Локаль подбирается по цепочке `ru-KZ → ru → notification.default-locale`.

Другой движок (Mustache, FreeMarker, внешний сервис шаблонов) — своя реализация `TemplateRenderer` с `@Primary` вместо `ThymeleafTemplateRenderer`. Её обязанности: экранировать переменные в HTML и никогда не выполнять выражения из их значений.

## 4. Политики и правила

| Правило | Как поменять |
|---|---|
| Число попыток и задержки | `notification.retry.*` — одновременно для Kafka и домена |
| Логика задержки (например, фиксированная) | бин `retryPolicy` в `UseCaseConfiguration` |
| Какие ошибки временные | в своём провайдере: `RetryableFailure` или `PermanentFailure`; SMTP — `SmtpErrorClassifier` |
| Лимиты частоты | `notification.delivery.rate-limits.<канал>` (обязательно с `categories`) |
| Какие категории обязательные и учитывают тихие часы | `Category.respectsQuietHours()` и соседние методы (`domain/notification/Category`) |
| Тихие часы | `QuietHoursPolicy` (сейчас — сдвиг на конец тихих часов в поясе получателя) |
| Выбор каналов (отписки, нет контакта) | `ChannelSelectionPolicy` |
| Итоговый статус уведомления | `NotificationStatusCalculator` |
| Порог зависания и потерянных сообщений | `notification.delivery.stale-sending-threshold`, `overdue-retry-grace` |

Политики домена — обычные классы без Spring, у каждой есть unit-тесты в `notification-domain/src/test`. Меняйте их вместе с тестами.

## 5. Заменить инфраструктуру

Остальные порты реализованы одним бином `@Component` в `infrastructure`. Заменить реализацию можно своим бином с `@Primary` или правкой класса.

| Порт | Реализация | Когда менять |
|---|---|---|
| `IdGenerator` | `TimeOrderedIdGenerator` (UUIDv7) | нужны другие id |
| `DeliveryDispatcher` | `KafkaDeliveryDispatcher` (через outbox) | другой брокер. **Обязательно** через outbox: постановка в очередь и запись в БД должны быть атомарны |
| `DomainEventPublisher` | `SpringDomainEventPublisher` | другой транспорт событий |
| `NotificationRepository`, `DeliveryRepository`, `TemplateRepository`, `PreferencesRepository` | JPA-адаптеры `out/persistence` | другое хранилище. `lockDueForDispatch`, `lockStaleSending`, `lockOverdueDispatches` обязаны пропускать заблокированные строки (`SKIP LOCKED`) |
| `NotificationReadModel`, `StatisticsReadModel` | JPA | отдельная read-модель (реплика, ClickHouse) |

Сценарии собираются в `bootstrap/config/UseCaseConfiguration`: там видно, какие порты нужны каждому сценарию.

## 6. События наружу

`StatusEventMapper` превращает доменные события в сообщения `notification.events.v1`. Новое событие — новая ветка `switch`: тип будет `kz.company.notification.<имя>.v1`. Несовместимые изменения данных выпускайте новой версией типа (`.v2`), а не правкой существующего. Не забудьте обновить [asyncapi.yaml](asyncapi.yaml).

## 7. Безопасность и мультиарендность

- **Откуда берётся отправитель** — `AuthenticatedClient` (`client_id` → `azp` → `sub`).
- **Какой scope нужен эндпоинту** — `SecurityConfiguration` и `ApiScopes`.
- **Видимость.** Сейчас каждый клиент видит только свои уведомления (`NotificationQueryService.loadOwned`). Админский доступ — отдельный scope и проверка в сценарии запроса.
- **Лимит запросов** считается в памяти экземпляра. Распределённый лимит (Redis, Bucket4j) — замена `ClientRateLimitFilter`, см. [ADR 0008](adr/0008-rate-limiting.md).

## Правила, которые проверяет сборка

`./mvnw verify` запускает ArchUnit (`ArchitectureTest`) и JaCoCo:

- домен зависит только от JDK; application — только от домена и `spring-tx`;
- входящие адаптеры (`in..`) не зависят от исходящих (`out..`) и наоборот; infrastructure не зависит от bootstrap;
- контроллеры — `*Controller` в `in.rest`, JPA-сущности — `*JpaEntity` в `out.persistence.entity`, отправители — `*ChannelSender` в `out.channel`;
- только конструкторное внедрение;
- покрытие строк domain и application не ниже 90% (`coverage.skip=false` включает проверку).

## Как тестировать расширение

| Уровень | Образец |
|---|---|
| Провайдер с HTTP API | `HttpWebhookProviderTest` (WireMock) |
| SMTP | `SmtpEmailProviderTest` (GreenMail) |
| Сценарий целиком без инфраструктуры | `application/src/test`: `ApplicationTestContext` и in-memory фейки портов |
| Полный путь REST/Kafka → провайдер → событие | `NotificationFlowIT`, `DeliveryFailureIT` (Testcontainers) |

Для ручной проверки повторов и DLT без реального провайдера используйте заглушку со сбоями: `notification.channels.<канал>.stub.failure-probability: 0.5`.
