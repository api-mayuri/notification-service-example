package kz.company.notification.application.port.in;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/** Предпочтения получателя для ответа API. */
public record PreferencesView(
        String recipientId,
        Set<String> unsubscribedChannels,
        Set<String> unsubscribedCategories,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        String timeZone,
        String locale,
        Instant updatedAt) {
}
