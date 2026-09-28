package kz.company.notification.domain.policy;

import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.preferences.RecipientPreferences;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QuietHoursPolicyTest {

    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");
    private final QuietHoursPolicy policy = new QuietHoursPolicy();

    private static RecipientPreferences withQuietHours(LocalTime start, LocalTime end) {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        preferences.apply(PreferencesChange.replaceWith(Set.of(), Set.of(), new QuietHours(start, end), ALMATY, null),
                Instant.EPOCH);
        return preferences;
    }

    @Test
    void marketingInsideOvernightQuietHoursMovesToMorning() {
        RecipientPreferences preferences = withQuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        Instant lateEvening = Instant.parse("2026-09-25T18:30:00Z"); // 23:30 в Алматы (UTC+5)

        Instant adjusted = policy.adjust(lateEvening, Category.MARKETING, preferences);

        assertThat(adjusted).isEqualTo(Instant.parse("2026-09-26T03:00:00Z")); // 08:00 следующего дня
    }

    @Test
    void marketingAfterMidnightMovesToSameMorning() {
        RecipientPreferences preferences = withQuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        Instant earlyMorning = Instant.parse("2026-09-25T20:00:00Z"); // 01:00 26.09 в Алматы

        assertThat(policy.adjust(earlyMorning, Category.MARKETING, preferences))
                .isEqualTo(Instant.parse("2026-09-26T03:00:00Z"));
    }

    @Test
    void daytimeQuietHoursMoveToTheirEnd() {
        RecipientPreferences preferences = withQuietHours(LocalTime.of(13, 0), LocalTime.of(14, 0));
        Instant lunch = Instant.parse("2026-09-25T08:15:00Z"); // 13:15 в Алматы

        assertThat(policy.adjust(lunch, Category.MARKETING, preferences))
                .isEqualTo(Instant.parse("2026-09-25T09:00:00Z"));
    }

    @Test
    void transactionalIgnoresQuietHours() {
        RecipientPreferences preferences = withQuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        Instant lateEvening = Instant.parse("2026-09-25T18:30:00Z");

        assertThat(policy.adjust(lateEvening, Category.TRANSACTIONAL, preferences)).isEqualTo(lateEvening);
        assertThat(policy.adjust(lateEvening, Category.SYSTEM, preferences)).isEqualTo(lateEvening);
    }

    @Test
    void outsideQuietHoursOrWithoutThemNothingChanges() {
        Instant noon = Instant.parse("2026-09-25T07:00:00Z"); // 12:00 в Алматы

        assertThat(policy.adjust(noon, Category.MARKETING, withQuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0))))
                .isEqualTo(noon);
        assertThat(policy.adjust(noon, Category.MARKETING, RecipientPreferences.defaults(new RecipientId("u"))))
                .isEqualTo(noon);
    }
}
