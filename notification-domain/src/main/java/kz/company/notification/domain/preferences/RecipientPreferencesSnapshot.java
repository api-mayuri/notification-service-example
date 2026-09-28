package kz.company.notification.domain.preferences;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.template.LocaleTag;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

/** Полное состояние агрегата {@link RecipientPreferences}. */
public record RecipientPreferencesSnapshot(
        RecipientId recipientId,
        Set<ChannelType> unsubscribedChannels,
        Set<Category> unsubscribedCategories,
        QuietHours quietHours,
        ZoneId timeZone,
        LocaleTag locale,
        Instant updatedAt,
        Long version) {
}
