package kz.company.notification.domain.notification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контакты получателя в «сыром» виде: тип контакта → значение. Проверка формата выполняется
 * при выборе канала, когда значение превращается в конкретный {@code ChannelAddress}.
 */
public record RecipientContacts(Map<ContactType, String> values) {

    public static final RecipientContacts NONE = new RecipientContacts(Map.of());

    public RecipientContacts {
        Map<ContactType, String> copy = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((type, value) -> {
                if (type != null && value != null && !value.isBlank()) {
                    copy.put(type, value.strip());
                }
            });
        }
        values = Collections.unmodifiableMap(copy);
    }

    public Optional<String> find(ContactType type) {
        return Optional.ofNullable(values.get(type));
    }

    /** Контакты из {@code preferred} перекрывают контакты из {@code this}. */
    public RecipientContacts overriddenBy(RecipientContacts preferred) {
        Map<ContactType, String> merged = new LinkedHashMap<>(values);
        merged.putAll(preferred.values());
        return new RecipientContacts(merged);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
