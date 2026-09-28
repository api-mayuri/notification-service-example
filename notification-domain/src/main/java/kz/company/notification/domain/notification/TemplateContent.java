package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Содержимое по шаблону. Локаль необязательна: если её нет, берётся из предпочтений
 * получателя, затем локаль по умолчанию.
 */
public record TemplateContent(TemplateKey templateKey, LocaleTag locale, Map<String, Object> variables)
        implements NotificationContent {

    public TemplateContent {
        Guard.notNull(templateKey, "templateKey");
        variables = variables == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(variables));
    }

    public Optional<LocaleTag> requestedLocale() {
        return Optional.ofNullable(locale);
    }
}
