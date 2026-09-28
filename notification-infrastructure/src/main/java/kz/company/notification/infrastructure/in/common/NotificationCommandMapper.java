package kz.company.notification.infrastructure.in.common;

import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.NotificationContent;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Перевод {@link NotificationRequestPayload} в {@link SendNotificationCommand}. Общий для REST и Kafka:
 * всё, что зависит от источника (отправитель, ключ идемпотентности, права), передаёт адаптер.
 */
@Component
public class NotificationCommandMapper {

    public SendNotificationCommand toCommand(NotificationRequestPayload payload, ClientId clientId,
                                             IdempotencyKey idempotencyKey, boolean rawContentAllowed) {
        return new SendNotificationCommand(
                clientId,
                idempotencyKey,
                new Recipient(new RecipientId(payload.recipient().id()), contacts(payload.recipient().contacts())),
                content(payload),
                channels(payload),
                parseEnum(Category.class, payload.category(), "category"),
                payload.priority() == null ? Priority.NORMAL : parseEnum(Priority.class, payload.priority(), "priority"),
                payload.sendAt() == null ? null : payload.sendAt().toInstant(),
                payload.metadata(),
                rawContentAllowed);
    }

    private static NotificationContent content(NotificationRequestPayload payload) {
        boolean hasTemplate = payload.template() != null;
        boolean hasContent = payload.content() != null;
        if (hasTemplate == hasContent) {
            throw new DomainValidationException("INVALID_CONTENT", "Exactly one of 'template' and 'content' must be set");
        }
        if (hasTemplate) {
            NotificationRequestPayload.TemplatePayload template = payload.template();
            return new TemplateContent(new TemplateKey(template.key()),
                    template.locale() == null || template.locale().isBlank() ? null : LocaleTag.of(template.locale()),
                    template.variables());
        }
        return new RawContent(payload.content().subject(), payload.content().text(), payload.content().html());
    }

    private static Set<ChannelType> channels(NotificationRequestPayload payload) {
        Set<ChannelType> channels = new LinkedHashSet<>();
        payload.channels().forEach(code -> channels.add(ChannelType.of(code)));
        return channels;
    }

    private static RecipientContacts contacts(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return RecipientContacts.NONE;
        }
        Map<ContactType, String> contacts = new LinkedHashMap<>();
        raw.forEach((type, value) -> contacts.put(new ContactType(type), value));
        return new RecipientContacts(contacts);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw DomainValidationException.invalidValue(field, "unknown value '" + value + "'");
        }
    }
}
