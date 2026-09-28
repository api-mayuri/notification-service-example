# Как добавить канал или заменить заглушку реальным провайдером

## A. Заменить заглушку реальным провайдером (пример: SMS через Twilio)

Интерфейс `SmsProvider` уже есть. Нужны один новый класс и одно значение в настройках, `SmsChannelSender` и домен не меняются.

1. **Класс провайдера** в `notification-infrastructure/.../out/channel/sms/TwilioSmsProvider.java`:

   ```java
   @Component
   @ConditionalOnProperty(prefix = "notification.channels.sms", name = "provider", havingValue = "twilio")
   public class TwilioSmsProvider implements SmsProvider {

       private final RestClient restClient;

       public TwilioSmsProvider(TwilioProperties properties) {        // @ConfigurationProperties("notification.channels.sms.twilio")
           this.restClient = RestClient.builder()
                   .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + properties.accountSid())
                   .defaultHeaders(headers -> headers.setBasicAuth(properties.accountSid(), properties.authToken()))
                   .build();
       }

       @Override
       public SendResult send(SmsMessage message) {
           try {
               return restClient.post().uri("/Messages.json")
                       .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                       .body("To=" + encode(message.to().value()) + "&From=...&Body=" + encode(message.text()))
                       .exchange((request, response) -> {
                           int status = response.getStatusCode().value();
                           if (status == 201) {
                               return SendResult.Success.real(response.bodyTo(TwilioMessage.class).sid());
                           }
                           if (status == 429 || status >= 500) {
                               return new SendResult.RetryableFailure("Twilio HTTP " + status);
                           }
                           return new SendResult.PermanentFailure("Twilio HTTP " + status); // неверный номер и т.п.
                       });
           } catch (ResourceAccessException networkError) {
               return new SendResult.RetryableFailure(networkError.getMessage());
           }
       }
   }
   ```

   Контракт провайдера описан в JavaDoc `SmsProvider`:
   - исключения наружу не бросать;
   - временные ошибки → `RetryableFailure`, постоянные → `PermanentFailure`;
   - `deliveryId` передавать провайдеру как ключ идемпотентности, если он это поддерживает.

2. **Настройки** — секреты только через переменные окружения:

   ```yaml
   notification:
     channels:
       sms:
         provider: twilio
         twilio:
           account-sid: ${TWILIO_ACCOUNT_SID}
           auth-token: ${TWILIO_AUTH_TOKEN}
   ```

3. **Тест** провайдера на WireMock по образцу `HttpWebhookProviderTest`.

После замены флаг `simulated` в доставках станет `false`: провайдер возвращает `Success.real(...)`.

То же для Telegram (Bot API `sendMessage`), WhatsApp (Cloud API) и push (FCM HTTP v1). Особенности:

- **Telegram:** 403 `bot was blocked by the user` — постоянная ошибка; 429 с `retry_after` — временная.
- **FCM:** `UNREGISTERED`, `INVALID_ARGUMENT` — постоянные; `UNAVAILABLE`, `INTERNAL`, 429 — временные.

## B. Добавить новый канал (пример: Viber)

Домен менять не нужно: `ChannelType`, `ContactType` и `ChannelAddress` — открытые типы.

1. **Отправитель канала** — один класс в `out/channel/viber/`:

   ```java
   @Component
   @ConditionalOnProperty(prefix = "notification.channels.viber", name = "enabled", havingValue = "true")
   public class ViberChannelSender implements ChannelSender {

       public static final ChannelType VIBER = new ChannelType("VIBER");
       private static final ContactType VIBER_ID = new ContactType("viberId");

       @Override public ChannelType channel() { return VIBER; }
       @Override public ContactType contactType() { return VIBER_ID; }
       @Override public ChannelAddress parseAddress(String raw) { return new ViberId(raw); } // свой record implements ChannelAddress

       @Override
       @ConcurrencyLimit(limitString = "${notification.channels.viber.max-concurrency:10}")
       public SendResult send(Delivery delivery) { /* вызов провайдера, классификация ответа */ }
   }
   ```

   Если у канала может быть несколько провайдеров, добавьте интерфейс `ViberProvider` и реализации по образцу SMS.

2. **Настройки:**

   ```yaml
   notification:
     channels:
       viber:
         enabled: true
         max-concurrency: 10
         concurrency: 3                # потребители обычной полосы
         high-priority-concurrency: 6  # потребители полосы HIGH
   ```

Больше ничего делать не нужно — остальное подключится автоматически:

- `SpringChannelSenderRegistry` найдёт бин и обернёт его circuit breaker'ом `channel-viber` и метриками.
- `DeliveryListenerRegistrar` зарегистрирует воркеры для `notification.delivery.viber.v1` и `notification.delivery.viber.high.v1` со своими consumer group.
- `KafkaConfiguration` создаст топики, DLT и retry-топики.
- API примет `"channels": ["VIBER"]` и контакт `"viberId"` в `recipient.contacts`.
- Шаблоны добавляются через API: `POST /api/v1/templates` с `"channel": "VIBER"`.

Проверьте, что ArchUnit-тест проходит: класс должен называться `*ChannelSender` и лежать в `..infrastructure.out.channel..`.
