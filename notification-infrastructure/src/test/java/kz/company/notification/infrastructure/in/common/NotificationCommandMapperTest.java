package kz.company.notification.infrastructure.in.common;

import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.TemplateContent;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCommandMapperTest {

    private final NotificationCommandMapper mapper = new NotificationCommandMapper();

    private static NotificationRequestPayload payload(NotificationRequestPayload.TemplatePayload template,
                                                      NotificationRequestPayload.ContentPayload content,
                                                      String category) {
        return new NotificationRequestPayload(
                new NotificationRequestPayload.RecipientPayload("user-1",
                        Map.of("email", "user@example.com", "telegramChatId", "123456")),
                template, content, List.of("email", "TELEGRAM"), category, "high",
                OffsetDateTime.parse("2026-10-01T09:00:00+05:00"), Map.of("correlationId", "c-1"), null);
    }

    @Test
    void mapsTemplateRequest() {
        SendNotificationCommand command = mapper.toCommand(payload(
                new NotificationRequestPayload.TemplatePayload("order-shipped", "ru-KZ", Map.of("orderId", "ORD-42")),
                null, "transactional"), new ClientId("orders"), new IdempotencyKey("k"), false);

        assertThat(command.channels()).containsExactlyInAnyOrder(ChannelType.EMAIL, ChannelType.TELEGRAM);
        assertThat(command.category()).isEqualTo(Category.TRANSACTIONAL);
        assertThat(command.priority()).isEqualTo(Priority.HIGH);
        assertThat(command.sendAt()).isEqualTo(OffsetDateTime.parse("2026-10-01T04:00:00Z").toInstant());
        assertThat(command.recipient().contacts().find(ContactType.TELEGRAM_CHAT_ID)).contains("123456");
        TemplateContent content = (TemplateContent) command.content();
        assertThat(content.locale().value()).isEqualTo("ru-KZ");
        assertThat(content.variables()).containsEntry("orderId", "ORD-42");
    }

    @Test
    void mapsRawContent() {
        SendNotificationCommand command = mapper.toCommand(payload(null,
                new NotificationRequestPayload.ContentPayload("s", "text", null), "SYSTEM"),
                new ClientId("orders"), null, true);

        assertThat(command.content()).isEqualTo(new RawContent("s", "text", null));
        assertThat(command.rawContentAllowed()).isTrue();
    }

    @Test
    void rejectsAmbiguousContentAndUnknownValues() {
        assertThatThrownBy(() -> mapper.toCommand(payload(null, null, "SYSTEM"), new ClientId("c"), null, false))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Exactly one");
        assertThatThrownBy(() -> mapper.toCommand(payload(null,
                new NotificationRequestPayload.ContentPayload(null, "t", null), "PROMO"), new ClientId("c"), null, false))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("category");
    }
}
