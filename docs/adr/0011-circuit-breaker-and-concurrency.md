# ADR 0011. Circuit breaker и лимит параллелизма на канал

- **Статус:** принято

- **Circuit breaker — Resilience4j 2.4** (`resilience4j-spring-boot4`, версия под Boot 4 существует). `ResilientChannelSender` — декоратор каждого `ChannelSender` в реестре. Breaker `channel-<канал>` с общей конфигурацией `channels`:
  - считает сбоями только временные ошибки и исключения;
  - постоянная ошибка (неверный адрес) — признак данных, а не здоровья провайдера;
  - пока breaker открыт, провайдер не вызывается, возвращается `RetryableFailure`, и доставка уходит в повтор.
- **Лимит параллелизма — `@ConcurrencyLimit` из Spring Framework 7** на методе `send` каждого отправителя (`limitString = ${notification.channels.<канал>.max-concurrency}`).
- **Метрики.** Декоратор пишет таймер `notification.channel.send{channel, outcome}` с исходами `success`, `simulated`, `retryable_failure`, `permanent_failure`, `error`, `circuit_open`.
- **Template Method для каналов не используется.** Общая логика заглушек вынесена в `StubProviderSupport` (композиция).
