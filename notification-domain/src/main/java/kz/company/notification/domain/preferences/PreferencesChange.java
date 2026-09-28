package kz.company.notification.domain.preferences;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.template.LocaleTag;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

/**
 * Частичное изменение предпочтений: {@code null} в поле означает «не менять».
 * Чтобы очистить тихие часы или язык, используются флаги {@code clear*}.
 */
public record PreferencesChange(
        Set<ChannelType> unsubscribedChannels,
        Set<Category> unsubscribedCategories,
        QuietHours quietHours,
        boolean clearQuietHours,
        ZoneId timeZone,
        LocaleTag locale,
        boolean clearLocale) {

    /** Полная замена (PUT): все поля заданы явно, отсутствующее значение очищается. */
    public static PreferencesChange replaceWith(Set<ChannelType> channels, Set<Category> categories,
                                                QuietHours quietHours, ZoneId timeZone, LocaleTag locale) {
        return new PreferencesChange(channels == null ? Set.of() : channels,
                categories == null ? Set.of() : categories,
                quietHours, quietHours == null, timeZone == null ? ZoneId.of("UTC") : timeZone,
                locale, locale == null);
    }

    Optional<Set<ChannelType>> optionalUnsubscribedChannels() {
        return Optional.ofNullable(unsubscribedChannels);
    }

    Optional<Set<Category>> optionalUnsubscribedCategories() {
        return Optional.ofNullable(unsubscribedCategories);
    }
}
