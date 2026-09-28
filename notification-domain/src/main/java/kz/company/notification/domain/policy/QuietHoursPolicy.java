package kz.company.notification.domain.policy;

import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.preferences.RecipientPreferences;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Сдвиг отправки на конец тихих часов получателя с учётом его часового пояса.
 * Применяется только к категориям, которые учитывают тихие часы (MARKETING);
 * TRANSACTIONAL и SYSTEM отправляются сразу.
 */
public final class QuietHoursPolicy {

    /** Время отправки с учётом тихих часов: {@code candidate} или конец тихих часов. */
    public Instant adjust(Instant candidate, Category category, RecipientPreferences preferences) {
        if (!category.respectsQuietHours()) {
            return candidate;
        }
        Optional<QuietHours> quietHours = preferences.quietHours();
        if (quietHours.isEmpty()) {
            return candidate;
        }
        ZonedDateTime local = candidate.atZone(preferences.timeZone());
        QuietHours hours = quietHours.get();
        if (!hours.contains(local.toLocalTime())) {
            return candidate;
        }
        LocalDate endDate = local.toLocalDate();
        if (hours.crossesMidnight() && !local.toLocalTime().isBefore(hours.start())) {
            endDate = endDate.plusDays(1);
        }
        return ZonedDateTime.of(endDate, hours.end(), preferences.timeZone()).toInstant();
    }
}
