# План: модули, пакеты, схема БД и брокера

## Модули и пакеты

```
notification-domain        kz.company.notification.domain
  common/        DomainEvent, DomainException(+Kind), Guard, Masking, AggregateEvents
  notification/  Notification, NotificationRequest, Snapshot, Id, ClientId, IdempotencyKey, Recipient,
                 RecipientContacts, ContactType, NotificationContent (TemplateContent | RawContent),
                 Category, Priority, NotificationStatus, DeduplicationKey, NotificationRepository, event/*
  delivery/      Delivery, DeliverySnapshot, DeliveryStatus, DeliveryFactory, DeliveryPlan, SendResult,
                 ChannelType, ChannelAddress (+ EmailAddress, PhoneNumber, TelegramChatId, DeviceToken,
                 WebhookUrl), RenderedMessage, SkipReason, RateWindowUsage, DeliveryRepository, event/*
  template/      Template, TemplateRevision, TemplateContentDefinition, TemplateKey, TemplateVersion,
                 LocaleTag, TemplateStatus, ResolvedTemplate, TemplateRepository
  preferences/   RecipientPreferences, PreferencesChange, QuietHours, PreferencesRepository
  policy/        ChannelSelectionPolicy, RetryPolicy, QuietHoursPolicy, NotificationStatusCalculator

notification-application   kz.company.notification.application
  port/in/       SendNotificationUseCase, SendBatchUseCase, CancelNotificationUseCase, GetNotificationQuery,
                 SearchNotificationsQuery, ManageTemplatesUseCase, PreviewTemplateUseCase,
                 ManagePreferencesUseCase, ProcessDeliveryUseCase, GetStatisticsQuery,
                 DispatchDueDeliveriesUseCase, RecoverStuckDeliveriesUseCase,
                 SynchronizeNotificationStatusUseCase, ReleaseExpiredIdempotencyKeysUseCase + команды и представления
  port/out/      ChannelSender, ChannelSenderRegistry, TemplateRenderer, RecipientResolver,
                 DomainEventPublisher, DeliveryDispatcher, IdGenerator, NotificationReadModel, StatisticsReadModel
  service/       реализации сценариев
  settings/      NotificationSettings, DeliverySettings, RateLimit

notification-infrastructure kz.company.notification.infrastructure
  in/rest        контроллеры, DTO, Problem Details, rate limit, scopes
  in/messaging   SendRequestListener, ChannelDeliveryListener, DeliveryDeadLetterHandler
  in/events      NotificationStatusListener (@ApplicationModuleListener)
  in/scheduling  NotificationScheduledJobs
  in/common      NotificationRequestPayload + маппер в команду (общий для REST и Kafka)
  out/persistence JPA-сущности, Spring Data, мапперы, адаптеры репозиториев, Specification, статистика
  out/messaging  KafkaDeliveryDispatcher, SpringDomainEventPublisher, StatusEventMapper
  out/channel    реестр, декоратор, email (SMTP), webhook (HTTP + HMAC), sms/telegram/whatsapp/push (заглушки)
  out/template   Thymeleaf + DatabaseTemplateResolver
  out/recipient  RequestOnlyRecipientResolver, HttpRecipientResolver
  messaging      топики, CloudEvents, ExternalMessage, сериализатор

notification-bootstrap      kz.company.notification.bootstrap
  config/        UseCaseConfiguration, KafkaConfiguration, SecurityConfiguration, PersistenceConfiguration,
                 OpenApiConfiguration, AsyncAndSchedulingConfiguration, DeliveryListenerRegistrar
  dev/           тестовые токены (только local и тесты)
  resources/     application*.yml, db/changelog (Liquibase)
```

## Схема БД (Liquibase)

| Таблица | Назначение | Ключевые индексы |
|---|---|---|
| `notification` | Уведомления | `uq_notification_client_idempotency_key (client_id, idempotency_key) WHERE key IS NOT NULL`, по client+created, recipient+created, dedup |
| `delivery` | Доставки | `uq (notification_id, channel)`; частичные индексы для планировщика (due, sending, retry), лимита частоты (`recipient, channel, sent_at`) |
| `template` | Шаблоны | `uq (template_key, channel, locale)` |
| `template_revision` | Версии | `uq (template_id, revision_number)`, `uq (template_id) WHERE status='ACTIVE'` |
| `recipient_preferences` | Предпочтения | PK `recipient_id` |
| `event_publication` | Outbox Spring Modulith | по `completion_date`, hash по `serialized_event` |

## Поток

```
REST ─┐                                   ┌─► notification.delivery.<канал>[.high].v1 ─► ChannelDeliveryListener
      ├─► SendNotificationUseCase ─► БД ─►│      │ временная ошибка ─► -retry-N ─► снова воркер
Kafka ┘   (Notification + Delivery +      │      │ FAILED ─► -dlt (parking lot)
           outbox в одной транзакции)     └─► notification.events.v1 ◄─ изменения статусов (outbox)
Планировщик: sendAt / тихие часы / лимит частоты → постановка в очередь; зависшие → назад в очередь.
```

## Этапы

Выполнены этапы 0–10 в одной сессии, с отступлением: заказчик попросил собрать сервис целиком, без остановки после этапа 0. Состояние и непроверенное — в `progress.md`.
