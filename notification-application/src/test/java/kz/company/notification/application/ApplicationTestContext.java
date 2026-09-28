package kz.company.notification.application;

import kz.company.notification.application.fake.FakeChannelSender;
import kz.company.notification.application.fake.FakeChannelSenderRegistry;
import kz.company.notification.application.fake.FakeRecipientResolver;
import kz.company.notification.application.fake.FakeTemplateRenderer;
import kz.company.notification.application.fake.InMemoryDeliveryRepository;
import kz.company.notification.application.fake.InMemoryNotificationRepository;
import kz.company.notification.application.fake.InMemoryPreferencesRepository;
import kz.company.notification.application.fake.InMemoryTemplateRepository;
import kz.company.notification.application.fake.MutableClock;
import kz.company.notification.application.fake.RecordingDeliveryDispatcher;
import kz.company.notification.application.fake.RecordingEventPublisher;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.service.CancelNotificationService;
import kz.company.notification.application.service.DeliveryProcessingService;
import kz.company.notification.application.service.DeliverySchedulingService;
import kz.company.notification.application.service.IdempotencyMaintenanceService;
import kz.company.notification.application.service.NotificationQueryService;
import kz.company.notification.application.service.NotificationStatusService;
import kz.company.notification.application.service.PreferencesService;
import kz.company.notification.application.service.SendBatchService;
import kz.company.notification.application.service.SendNotificationService;
import kz.company.notification.application.service.TemplateLookup;
import kz.company.notification.application.service.TemplateManagementService;
import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.application.settings.NotificationSettings;
import kz.company.notification.application.settings.RateLimit;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.PhoneNumber;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.NotificationContent;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.policy.RetryPolicy;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Сценарии приложения, собранные на in-memory реализациях портов. */
public final class ApplicationTestContext {

    public static final Instant START = Instant.parse("2026-09-25T06:00:00Z");
    public static final ClientId CLIENT = new ClientId("orders-service");
    public static final RecipientId RECIPIENT = new RecipientId("user-1");
    public static final TemplateKey ORDER_SHIPPED = new TemplateKey("order-shipped");

    public final MutableClock clock = new MutableClock(START);
    public final InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
    public final InMemoryDeliveryRepository deliveries = new InMemoryDeliveryRepository();
    public final InMemoryTemplateRepository templates = new InMemoryTemplateRepository();
    public final InMemoryPreferencesRepository preferences = new InMemoryPreferencesRepository();
    public final FakeTemplateRenderer renderer = new FakeTemplateRenderer();
    public final FakeRecipientResolver recipientResolver = new FakeRecipientResolver();
    public final RecordingEventPublisher events = new RecordingEventPublisher();
    public final RecordingDeliveryDispatcher dispatcher = new RecordingDeliveryDispatcher();
    public final FakeChannelSender email = new FakeChannelSender(ChannelType.EMAIL, ContactType.EMAIL,
            value -> (ChannelAddress) new EmailAddress(value));
    public final FakeChannelSender sms = new FakeChannelSender(ChannelType.SMS, ContactType.PHONE,
            value -> (ChannelAddress) new PhoneNumber(value));
    public final FakeChannelSenderRegistry registry = new FakeChannelSenderRegistry().register(email).register(sms);
    public final RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ofSeconds(10), 2.0, Duration.ofMinutes(5),
            0.0, () -> 0.5);
    public final DeliverySettings deliverySettings = new DeliverySettings(Duration.ofMinutes(5), Duration.ofMinutes(10),
            100, Map.of(ChannelType.SMS, new RateLimit(2, Duration.ofHours(1), null)), Duration.ofHours(24));

    private final AtomicLong ids = new AtomicLong();

    public final SendNotificationService sendService;
    public final SendBatchService batchService;
    public final DeliveryProcessingService processingService;
    public final DeliverySchedulingService schedulingService;
    public final CancelNotificationService cancelService;
    public final NotificationQueryService queryService;
    public final NotificationStatusService statusService;
    public final TemplateManagementService templateService;
    public final PreferencesService preferencesService;
    public final IdempotencyMaintenanceService idempotencyService;

    public ApplicationTestContext() {
        LocaleTag defaultLocale = LocaleTag.of("ru");
        sendService = new SendNotificationService(notifications, deliveries, preferences, registry,
                new TemplateLookup(templates, defaultLocale), renderer, recipientResolver, dispatcher, events,
                this::nextId, clock, new NotificationSettings(defaultLocale));
        batchService = new SendBatchService(sendService);
        processingService = new DeliveryProcessingService(deliveries, registry, events, retryPolicy, deliverySettings,
                TransactionOperations.withoutTransaction(), clock);
        schedulingService = new DeliverySchedulingService(deliveries, dispatcher, events, retryPolicy, deliverySettings,
                clock);
        cancelService = new CancelNotificationService(notifications, deliveries, events, clock);
        queryService = new NotificationQueryService(notifications, deliveries, (criteria, page) -> {
            throw new UnsupportedOperationException("read model is tested in infrastructure");
        });
        statusService = new NotificationStatusService(notifications, deliveries, events, clock);
        templateService = new TemplateManagementService(templates, renderer, this::nextId, clock);
        preferencesService = new PreferencesService(preferences, clock);
        idempotencyService = new IdempotencyMaintenanceService(notifications, deliverySettings, clock);
    }

    public UUID nextId() {
        return new UUID(0, ids.incrementAndGet());
    }

    /** Активный шаблон order-shipped для канала и локали. */
    public Template givenActiveTemplate(ChannelType channel, String locale, Duration deduplicationWindow) {
        Template template = Template.create(new TemplateId(nextId()), ORDER_SHIPPED, channel, LocaleTag.of(locale),
                new TemplateContentDefinition("Заказ ${orderId}", "Заказ ${orderId} отправлен", "text ${orderId}",
                        Set.of("orderId")), deduplicationWindow, clock.instant());
        template.activate(TemplateVersion.FIRST, clock.instant());
        return templates.save(template);
    }

    /** Активный шаблон для канала в локали ru, если его ещё нет. */
    public void givenActiveTemplateOnce(ChannelType channel) {
        if (templates.find(ORDER_SHIPPED, channel, LocaleTag.of("ru")).isEmpty()) {
            givenActiveTemplate(channel, "ru", null);
        }
    }

    public static RecipientContacts contacts() {
        return new RecipientContacts(Map.of(ContactType.EMAIL, "user@example.com", ContactType.PHONE, "+77011234567"));
    }

    public static SendNotificationCommand command(String idempotencyKey, Category category, ChannelType... channels) {
        return command(idempotencyKey, category, new TemplateContent(ORDER_SHIPPED, null, Map.of("orderId", "ORD-42")),
                contacts(), channels);
    }

    public static SendNotificationCommand command(String idempotencyKey, Category category, NotificationContent content,
                                                  RecipientContacts contacts, ChannelType... channels) {
        return new SendNotificationCommand(CLIENT, idempotencyKey == null ? null : new IdempotencyKey(idempotencyKey),
                new Recipient(RECIPIENT, contacts), content, Set.of(channels), category, Priority.NORMAL, null,
                Map.of("correlationId", "c-1"), false);
    }
}
