# ADR 0005. Топология Kafka, повторы, приоритеты и порядок

- **Статус:** принято

## Топики

| Топик | Ключ | Назначение |
|---|---|---|
| `notification.send.requests.v1` (+ `-dlt`) | recipientId | Входящие команды (CloudEvents, binary mode) |
| `notification.delivery.<канал>.v1` (+ `-retry-N`, `-dlt`) | recipientId | Доставки канала, обычная полоса |
| `notification.delivery.<канал>.high.v1` (+ `-retry-N`, `-dlt`) | recipientId | Доставки канала, приоритет HIGH |
| `notification.events.v1` | notificationId | События статусов наружу |

Топики объявляются бинами (`KafkaAdmin.NewTopics`). Retry-топики создаёт `RetryTopicConfiguration`, автосоздание на брокере выключено. Параметры (`notification.kafka.*`):

- **prod:** RF 3, `min.insync.replicas=2`, 12 партиций;
- **dev:** RF 1, 3 партиции;
- **retention:** 7 дней для основных топиков, 30 дней для DLT.

## Повторы: кто считает попытки

Противоречие промпта: попытки могут считаться в трёх местах (домен, `@RetryableTopic`, `@Retryable`). Решение:

- **Источник истины — домен.** `Delivery` считает попытки, `RetryPolicy` решает, повторять ли, и вычисляет `nextAttemptAt` с jitter.
- **Kafka — транспорт задержки.** Если после попытки доставка в `RETRY_SCHEDULED`, воркер бросает `RetryableDeliveryException`, и запись уходит в retry-топик с экспоненциальной задержкой. Настройки (`maxAttempts`, `initialDelay`, `multiplier`, `maxDelay`) общие с доменной политикой (`notification.retry.*`). Jitter в retry-топиках не применяется: у каждого топика фиксированная задержка.
- **Окончательный отказ.** Если доставка в `FAILED` (постоянная ошибка или исчерпаны попытки), воркер бросает `DeliveryFailedException`. Это исключение в списке `notRetryOn`, поэтому запись сразу уходит в DLT. `DeliveryDeadLetterHandler` на всякий случай переводит доставку в `FAILED`, если она почему-то ещё не там.
- **`@Retryable` из Spring Framework 7** используется только для коротких сетевых повторов внутри одной попытки (`WebhookHttpClient`).
- **Страховка.** Если запись о повторе потерялась, а `nextAttemptAt` сильно просрочен, планировщик вернёт доставку в очередь (`RecoverStuckDeliveriesUseCase`).

## Приоритеты

В Kafka нет приоритетов сообщений. HIGH идёт в отдельные топики `…high.v1` с отдельной consumer group и бо́льшим числом потребителей (`high-priority-concurrency`). Полоса выбирается по `Priority`: LOW и NORMAL идут в обычную.

## Порядок

Ключ `recipientId` сохраняет порядок сообщений одного получателя в партиции. Неблокирующие повторы этот порядок нарушают: повторяемое сообщение уходит в retry-топик, а следующие обрабатываются. Для уведомлений это допустимо — они независимы, а блокировать всю партицию из-за одного недоступного адресата хуже.

## Воркеры по каналам

На каждый включённый канал и полосу регистрируется бин `ChannelDeliveryListener` (`DeliveryListenerRegistrar`). У каждого свой топик, consumer group (`notification-service.delivery.<канал>[.high]`) и concurrency. Медленный SMS-шлюз не тормозит email: у каналов раздельные партиции, группы, лимиты параллелизма (`@ConcurrencyLimit`) и circuit breaker.

## Надёжность producer и consumer

- **Producer:** `enable.idempotence=true`, `acks=all`. Kafka-транзакции не используются: БД и Kafka согласуются через outbox.
- **Consumer:** `enable.auto.commit=false`, `AckMode.RECORD` — offset фиксируется после обработки и сохранения статуса. `ErrorHandlingDeserializer` оборачивает `StringDeserializer`, JSON разбирается в слушателе. Битые сообщения уходят в DLT и не блокируют партицию.
- **Входящие команды:** `DefaultErrorHandler` с экспоненциальным backoff (5 повторов) и `DeadLetterPublishingRecoverer` в `-dlt`. Невалидные и отклонённые бизнес-правилами сообщения попадают туда сразу.

## Следующий шаг

Schema Registry (Confluent или Apicurio) для схем сообщений не входит в объём работ. Контракты описаны в `docs/asyncapi.yaml`.
