package kz.company.notification.application.service;

import kz.company.notification.application.exception.ChannelNotAvailableException;
import kz.company.notification.application.exception.IdempotencyKeyReusedException;
import kz.company.notification.application.exception.RawContentNotAllowedException;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.application.port.in.SendNotificationUseCase;
import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.application.port.out.DeliveryDispatcher;
import kz.company.notification.application.port.out.DomainEventPublisher;
import kz.company.notification.application.port.out.IdGenerator;
import kz.company.notification.application.port.out.RecipientResolver;
import kz.company.notification.application.port.out.TemplateRenderer;
import kz.company.notification.application.settings.NotificationSettings;
import kz.company.notification.domain.common.DomainEvent;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryFactory;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryPlan;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.notification.DeduplicationKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRepository;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.policy.ChannelSelection;
import kz.company.notification.domain.policy.ChannelSelectionPolicy;
import kz.company.notification.domain.policy.QuietHoursPolicy;
import kz.company.notification.domain.preferences.PreferencesRepository;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.ResolvedTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Сценарий приёма уведомления. Всё в одной транзакции: уведомление, доставки и записи
 * outbox (события и постановка в топики каналов).
 */
public class SendNotificationService implements SendNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final DeliveryRepository deliveryRepository;
    private final PreferencesRepository preferencesRepository;
    private final ChannelSenderRegistry channelRegistry;
    private final TemplateLookup templateLookup;
    private final TemplateRenderer templateRenderer;
    private final RecipientResolver recipientResolver;
    private final DeliveryDispatcher deliveryDispatcher;
    private final DomainEventPublisher eventPublisher;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final NotificationSettings settings;
    private final DeliveryFactory deliveryFactory;
    private final ChannelSelectionPolicy channelSelectionPolicy = new ChannelSelectionPolicy();
    private final QuietHoursPolicy quietHoursPolicy = new QuietHoursPolicy();

    public SendNotificationService(NotificationRepository notificationRepository,
                                   DeliveryRepository deliveryRepository,
                                   PreferencesRepository preferencesRepository,
                                   ChannelSenderRegistry channelRegistry,
                                   TemplateLookup templateLookup,
                                   TemplateRenderer templateRenderer,
                                   RecipientResolver recipientResolver,
                                   DeliveryDispatcher deliveryDispatcher,
                                   DomainEventPublisher eventPublisher,
                                   IdGenerator idGenerator,
                                   Clock clock,
                                   NotificationSettings settings) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferencesRepository = preferencesRepository;
        this.channelRegistry = channelRegistry;
        this.templateLookup = templateLookup;
        this.templateRenderer = templateRenderer;
        this.recipientResolver = recipientResolver;
        this.deliveryDispatcher = deliveryDispatcher;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.settings = settings;
        this.deliveryFactory = new DeliveryFactory(() -> new DeliveryId(idGenerator.newId()));
    }

    @Override
    @Transactional
    public SendNotificationResult send(SendNotificationCommand command) {
        Instant now = clock.instant();
        String fingerprint = CommandFingerprint.of(command);

        Optional<SendNotificationResult> replay = findReplay(command, fingerprint);
        if (replay.isPresent()) {
            return replay.get();
        }
        if (command.content() instanceof RawContent && !command.rawContentAllowed()) {
            throw new RawContentNotAllowedException();
        }
        Set<ChannelType> channels = new TreeSet<>(command.channels());
        requireEnabled(channels);

        RecipientPreferences preferences = preferencesRepository.findByRecipientId(command.recipient().id())
                .orElseGet(() -> RecipientPreferences.defaults(command.recipient().id()));
        RecipientContacts contacts = resolveContacts(command, channels);
        ChannelSelection selection = channelSelectionPolicy.select(channels, command.category(), preferences,
                contacts, channelRegistry);

        RenderingResult rendering = render(command, selection, preferences);
        DeduplicationKey deduplicationKey = null;
        if (rendering.deduplicationWindow() != null && command.content() instanceof TemplateContent template) {
            deduplicationKey = DeduplicationKey.of(command.recipient().id(), template.templateKey(),
                    template.variables(), channels);
            Optional<Notification> duplicate = notificationRepository.findLatestByDeduplicationKey(deduplicationKey,
                    now.minus(rendering.deduplicationWindow()));
            if (duplicate.isPresent()) {
                return new SendNotificationResult(duplicate.get().id(), duplicate.get().status(),
                        SendNotificationResult.Outcome.DEDUPLICATED);
            }
        }

        NotificationRequest request = new NotificationRequest(command.clientId(), command.idempotencyKey(), fingerprint,
                new Recipient(command.recipient().id(), RecipientContacts.NONE), command.content(), channels,
                command.category(), command.priority(), command.sendAt(), command.metadata());
        Notification notification = Notification.accept(new NotificationId(idGenerator.newId()), request,
                deduplicationKey, now);
        List<Delivery> deliveries = deliveryFactory.create(notification,
                plan(selection, rendering.messages(), command, preferences, now), now);
        deliveries.stream().filter(delivery -> delivery.isDue(now)).forEach(delivery -> delivery.markDispatched(now));
        notification.refreshStatus(deliveries.stream().map(Delivery::status).toList(), now);

        List<DomainEvent> events = new ArrayList<>(notification.pullDomainEvents());
        deliveries.forEach(delivery -> events.addAll(delivery.pullDomainEvents()));

        Notification saved = notificationRepository.save(notification);
        List<Delivery> savedDeliveries = deliveryRepository.saveAll(deliveries);
        savedDeliveries.stream()
                .filter(delivery -> delivery.status() == DeliveryStatus.PENDING && delivery.dispatchedAt().isPresent())
                .forEach(deliveryDispatcher::dispatch);
        eventPublisher.publish(events);
        return new SendNotificationResult(saved.id(), saved.status(), SendNotificationResult.Outcome.CREATED);
    }

    private Optional<SendNotificationResult> findReplay(SendNotificationCommand command, String fingerprint) {
        if (command.idempotencyKey() == null) {
            return Optional.empty();
        }
        return notificationRepository.findByIdempotencyKey(command.clientId(), command.idempotencyKey())
                .map(existing -> {
                    if (!existing.hasSameRequestAs(fingerprint)) {
                        throw new IdempotencyKeyReusedException(command.idempotencyKey());
                    }
                    return new SendNotificationResult(existing.id(), existing.status(),
                            SendNotificationResult.Outcome.REPLAYED);
                });
    }

    private void requireEnabled(Set<ChannelType> channels) {
        Set<ChannelType> unavailable = new TreeSet<>(channels);
        unavailable.removeAll(channelRegistry.enabledChannels());
        if (!unavailable.isEmpty()) {
            throw new ChannelNotAvailableException(unavailable, new TreeSet<>(channelRegistry.enabledChannels()));
        }
    }

    /** Контакты из запроса; сервис пользователей вызывается, только если чего-то не хватает. */
    private RecipientContacts resolveContacts(SendNotificationCommand command, Set<ChannelType> channels) {
        RecipientContacts provided = command.recipient().contacts();
        boolean complete = channels.stream()
                .map(channelRegistry::find)
                .flatMap(Optional::stream)
                .map(ChannelSender::contactType)
                .allMatch(type -> provided.find(type).isPresent());
        if (complete) {
            return provided;
        }
        return recipientResolver.resolve(command.recipient().id()).overriddenBy(provided);
    }

    private RenderingResult render(SendNotificationCommand command, ChannelSelection selection,
                                   RecipientPreferences preferences) {
        Map<ChannelType, RenderedMessage> messages = new LinkedHashMap<>();
        Duration deduplicationWindow = null;
        for (ChannelSelection.Selected selected : selection.selected()) {
            switch (command.content()) {
                case TemplateContent template -> {
                    LocaleTag locale = template.requestedLocale()
                            .or(preferences::locale)
                            .orElse(settings.defaultLocale());
                    ResolvedTemplate resolved = templateLookup.resolveActive(template.templateKey(), selected.channel(), locale);
                    resolved.revision().requireVariables(template.templateKey(), template.variables());
                    messages.put(selected.channel(), templateRenderer.render(resolved, template.variables()));
                    Duration window = resolved.template().deduplicationWindow().orElse(null);
                    if (window != null && (deduplicationWindow == null || window.compareTo(deduplicationWindow) > 0)) {
                        deduplicationWindow = window;
                    }
                }
                case RawContent raw -> messages.put(selected.channel(),
                        RenderedMessage.raw(raw.subject(), raw.text(), raw.html()));
            }
        }
        return new RenderingResult(messages, deduplicationWindow);
    }

    private List<DeliveryPlan> plan(ChannelSelection selection, Map<ChannelType, RenderedMessage> messages,
                                    SendNotificationCommand command, RecipientPreferences preferences, Instant now) {
        Instant base = command.sendAt() != null && command.sendAt().isAfter(now) ? command.sendAt() : now;
        Instant scheduledFor = quietHoursPolicy.adjust(base, command.category(), preferences);
        Set<DeliveryPlan> plans = new LinkedHashSet<>();
        selection.selected().forEach(selected -> plans.add(new DeliveryPlan.Deliverable(selected.channel(),
                selected.address(), messages.get(selected.channel()), scheduledFor)));
        selection.skipped().forEach(skipped -> plans.add(new DeliveryPlan.Skip(skipped.channel(), skipped.reason())));
        return List.copyOf(plans);
    }

    private record RenderingResult(Map<ChannelType, RenderedMessage> messages, Duration deduplicationWindow) {
    }
}
