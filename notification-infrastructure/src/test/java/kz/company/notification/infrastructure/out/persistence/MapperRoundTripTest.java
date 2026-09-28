package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryFactory;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliveryPlan;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;
import kz.company.notification.infrastructure.out.persistence.mapper.DeliveryMapper;
import kz.company.notification.infrastructure.out.persistence.mapper.JsonText;
import kz.company.notification.infrastructure.out.persistence.mapper.NotificationMapper;
import kz.company.notification.infrastructure.out.persistence.mapper.PreferencesMapper;
import kz.company.notification.infrastructure.out.persistence.mapper.TemplateMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapperRoundTripTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Test
    void notificationSurvivesRoundTrip() {
        NotificationMapper mapper = new NotificationMapper(new JsonText(JsonMapper.builder().build()));
        Notification notification = Notification.accept(new NotificationId(UUID.randomUUID()),
                new NotificationRequest(new ClientId("orders"), new IdempotencyKey("k-1"), "fp",
                        new Recipient(new RecipientId("user-1"), RecipientContacts.NONE),
                        new TemplateContent(new TemplateKey("order-shipped"), LocaleTag.of("ru-KZ"),
                                Map.of("orderId", "ORD-42", "items", List.of("a", "b"))),
                        Set.of(ChannelType.EMAIL, ChannelType.SMS), Category.MARKETING, Priority.HIGH, NOW,
                        Map.of("correlationId", "c-1")),
                null, NOW);

        Notification restored = mapper.toDomain(mapper.toEntity(notification));

        assertThat(restored.snapshot()).isEqualTo(notification.snapshot());
    }

    @Test
    void deliverySurvivesRoundTripIncludingSkipped() {
        ChannelSenderRegistry registry = mock(ChannelSenderRegistry.class);
        ChannelSender emailSender = mock(ChannelSender.class);
        when(registry.find(ChannelType.EMAIL)).thenReturn(Optional.of(emailSender));
        when(registry.find(ChannelType.SMS)).thenReturn(Optional.empty());
        when(emailSender.parseAddress("user@example.com")).thenReturn(new EmailAddress("user@example.com"));
        DeliveryMapper mapper = new DeliveryMapper(registry);
        Notification notification = Notification.accept(new NotificationId(UUID.randomUUID()),
                new NotificationRequest(new ClientId("orders"), null, null,
                        new Recipient(new RecipientId("user-1"), RecipientContacts.NONE),
                        new TemplateContent(new TemplateKey("welcome"), null, Map.of()),
                        Set.of(ChannelType.EMAIL, ChannelType.SMS), Category.TRANSACTIONAL, null, null, null),
                null, NOW);
        List<Delivery> deliveries = new DeliveryFactory(() -> new DeliveryId(UUID.randomUUID())).create(notification,
                List.of(new DeliveryPlan.Deliverable(ChannelType.EMAIL, new EmailAddress("user@example.com"),
                                new RenderedMessage("s", "t", "<p>h</p>", "welcome", 3, "ru"), NOW),
                        new DeliveryPlan.Skip(ChannelType.SMS, SkipReason.NO_CONTACT)), NOW);

        for (Delivery delivery : deliveries) {
            assertThat(mapper.toDomain(mapper.toEntity(delivery)).snapshot()).isEqualTo(delivery.snapshot());
        }
    }

    @Test
    void templateAndPreferencesSurviveRoundTrip() {
        Template template = Template.create(new TemplateId(UUID.randomUUID()), new TemplateKey("welcome"),
                ChannelType.EMAIL, LocaleTag.of("ru"),
                new TemplateContentDefinition("s", "<p>b</p>", "t", Set.of("name", "code")), Duration.ofMinutes(5), NOW);
        template.addRevision(new TemplateContentDefinition(null, "v2", null, Set.of()), NOW);
        template.activate(new TemplateVersion(2), NOW);
        TemplateMapper templateMapper = new TemplateMapper();

        assertThat(templateMapper.toDomain(templateMapper.toEntity(template)).snapshot()).isEqualTo(template.snapshot());

        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        preferences.apply(PreferencesChange.replaceWith(Set.of(ChannelType.SMS, ChannelType.PUSH), Set.of(Category.MARKETING),
                new QuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0)), ZoneId.of("Asia/Almaty"), LocaleTag.of("kk")), NOW);
        PreferencesMapper preferencesMapper = new PreferencesMapper();

        assertThat(preferencesMapper.toDomain(preferencesMapper.toEntity(preferences)).snapshot())
                .isEqualTo(preferences.snapshot());
    }
}
