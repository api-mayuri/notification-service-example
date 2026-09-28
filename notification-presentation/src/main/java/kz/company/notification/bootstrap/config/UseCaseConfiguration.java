package kz.company.notification.bootstrap.config;

import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.application.port.out.DeliveryDispatcher;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.application.port.out.IdGenerator;
import kz.company.notification.application.port.out.NotificationReadModel;
import kz.company.notification.application.port.out.RecipientResolver;
import kz.company.notification.application.port.out.StatisticsReadModel;
import kz.company.notification.application.port.out.TemplateRenderer;
import kz.company.notification.application.service.CancelNotificationService;
import kz.company.notification.application.service.DeliveryProcessingService;
import kz.company.notification.application.service.DeliverySchedulingService;
import kz.company.notification.application.service.IdempotencyMaintenanceService;
import kz.company.notification.application.service.NotificationQueryService;
import kz.company.notification.application.service.NotificationStatusService;
import kz.company.notification.application.service.PreferencesService;
import kz.company.notification.application.service.SendBatchService;
import kz.company.notification.application.service.SendNotificationService;
import kz.company.notification.application.service.StatisticsService;
import kz.company.notification.application.service.TemplateLookup;
import kz.company.notification.application.service.TemplateManagementService;
import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.application.settings.NotificationSettings;
import kz.company.notification.application.settings.RateLimit;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.NotificationRepository;
import kz.company.notification.domain.policy.RetryPolicy;
import kz.company.notification.domain.preferences.PreferencesRepository;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сборка сценариев приложения из портов. Классы сценариев не знают о Spring-стереотипах;
 * транзакции включаются аннотацией {@code @Transactional} на их методах.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RetryPolicy retryPolicy(NotificationProperties properties) {
        NotificationProperties.Retry retry = properties.retry();
        return new RetryPolicy(retry.maxAttempts(), retry.initialDelay(), retry.multiplier(), retry.maxDelay(),
                retry.jitter(), () -> ThreadLocalRandom.current().nextDouble());
    }

    @Bean
    NotificationSettings notificationSettings(NotificationProperties properties) {
        return new NotificationSettings(LocaleTag.of(properties.defaultLocale()));
    }

    @Bean
    DeliverySettings deliverySettings(NotificationProperties properties) {
        NotificationProperties.Delivery delivery = properties.delivery();
        Map<ChannelType, RateLimit> rateLimits = new LinkedHashMap<>();
        delivery.rateLimits().forEach((channel, limit) -> rateLimits.put(ChannelType.of(channel),
                new RateLimit(limit.max(), limit.window(), categories(limit.categories()))));
        return new DeliverySettings(delivery.staleSendingThreshold(), delivery.overdueRetryGrace(), delivery.batchSize(),
                rateLimits, delivery.idempotencyKeyRetention());
    }

    @Bean
    TemplateLookup templateLookup(TemplateRepository templateRepository, NotificationSettings settings) {
        return new TemplateLookup(templateRepository, settings.defaultLocale());
    }

    @Bean
    SendNotificationService sendNotificationService(NotificationRepository notificationRepository,
                                                    DeliveryRepository deliveryRepository,
                                                    PreferencesRepository preferencesRepository,
                                                    ChannelSenderRegistry channelSenderRegistry,
                                                    TemplateLookup templateLookup,
                                                    TemplateRenderer templateRenderer,
                                                    RecipientResolver recipientResolver,
                                                    DeliveryDispatcher deliveryDispatcher,
                                                    DomainEventPublisher domainEventPublisher,
                                                    IdGenerator idGenerator,
                                                    Clock clock,
                                                    NotificationSettings settings) {
        return new SendNotificationService(notificationRepository, deliveryRepository, preferencesRepository,
                channelSenderRegistry, templateLookup, templateRenderer, recipientResolver, deliveryDispatcher,
                domainEventPublisher, idGenerator, clock, settings);
    }

    @Bean
    SendBatchService sendBatchService(SendNotificationService sendNotificationService) {
        return new SendBatchService(sendNotificationService);
    }

    @Bean
    CancelNotificationService cancelNotificationService(NotificationRepository notificationRepository,
                                                        DeliveryRepository deliveryRepository,
                                                        DomainEventPublisher domainEventPublisher, Clock clock) {
        return new CancelNotificationService(notificationRepository, deliveryRepository, domainEventPublisher, clock);
    }

    @Bean
    NotificationQueryService notificationQueryService(NotificationRepository notificationRepository,
                                                      DeliveryRepository deliveryRepository,
                                                      NotificationReadModel readModel) {
        return new NotificationQueryService(notificationRepository, deliveryRepository, readModel);
    }

    @Bean
    DeliveryProcessingService deliveryProcessingService(DeliveryRepository deliveryRepository,
                                                        ChannelSenderRegistry channelSenderRegistry,
                                                        DomainEventPublisher domainEventPublisher,
                                                        RetryPolicy retryPolicy, DeliverySettings deliverySettings,
                                                        TransactionOperations transactionOperations, Clock clock) {
        return new DeliveryProcessingService(deliveryRepository, channelSenderRegistry, domainEventPublisher,
                retryPolicy, deliverySettings, transactionOperations, clock);
    }

    @Bean
    DeliverySchedulingService deliverySchedulingService(DeliveryRepository deliveryRepository,
                                                        DeliveryDispatcher deliveryDispatcher,
                                                        DomainEventPublisher domainEventPublisher,
                                                        RetryPolicy retryPolicy, DeliverySettings deliverySettings,
                                                        Clock clock) {
        return new DeliverySchedulingService(deliveryRepository, deliveryDispatcher, domainEventPublisher, retryPolicy,
                deliverySettings, clock);
    }

    @Bean
    NotificationStatusService notificationStatusService(NotificationRepository notificationRepository,
                                                        DeliveryRepository deliveryRepository,
                                                        DomainEventPublisher domainEventPublisher, Clock clock) {
        return new NotificationStatusService(notificationRepository, deliveryRepository, domainEventPublisher, clock);
    }

    @Bean
    TemplateManagementService templateManagementService(TemplateRepository templateRepository,
                                                        TemplateRenderer templateRenderer, IdGenerator idGenerator,
                                                        Clock clock) {
        return new TemplateManagementService(templateRepository, templateRenderer, idGenerator, clock);
    }

    @Bean
    PreferencesService preferencesService(PreferencesRepository preferencesRepository, Clock clock) {
        return new PreferencesService(preferencesRepository, clock);
    }

    @Bean
    StatisticsService statisticsService(StatisticsReadModel statisticsReadModel) {
        return new StatisticsService(statisticsReadModel);
    }

    @Bean
    IdempotencyMaintenanceService idempotencyMaintenanceService(NotificationRepository notificationRepository,
                                                                DeliverySettings deliverySettings, Clock clock) {
        return new IdempotencyMaintenanceService(notificationRepository, deliverySettings, clock);
    }

    private static Set<Category> categories(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<Category> categories = EnumSet.noneOf(Category.class);
        names.forEach(name -> categories.add(Category.valueOf(name.strip().toUpperCase(Locale.ROOT))));
        return categories;
    }
}
