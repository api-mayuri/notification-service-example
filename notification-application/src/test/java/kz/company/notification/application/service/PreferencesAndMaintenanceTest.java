package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.PreferencesView;
import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.in.StatisticsRow;
import kz.company.notification.application.port.in.StatisticsView;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.template.LocaleTag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static kz.company.notification.application.ApplicationTestContext.CLIENT;
import static kz.company.notification.application.ApplicationTestContext.RECIPIENT;
import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferencesAndMaintenanceTest {

    private final ApplicationTestContext context = new ApplicationTestContext();

    @Test
    void preferencesDefaultsAndChanges() {
        PreferencesView defaults = context.preferencesService.get(RECIPIENT);
        PreferencesView changed = context.preferencesService.change(RECIPIENT, PreferencesChange.replaceWith(
                Set.of(ChannelType.SMS), Set.of(Category.MARKETING), new QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0)),
                ZoneId.of("Asia/Almaty"), LocaleTag.of("kk")));

        assertThat(defaults.timeZone()).isEqualTo("Z");
        assertThat(defaults.updatedAt()).isNull();
        assertThat(changed.unsubscribedChannels()).containsExactly("SMS");
        assertThat(changed.unsubscribedCategories()).containsExactly("MARKETING");
        assertThat(changed.quietHoursStart()).isEqualTo(LocalTime.of(22, 0));
        assertThat(changed.timeZone()).isEqualTo("Asia/Almaty");
        assertThat(changed.locale()).isEqualTo("kk");
        assertThat(context.preferencesService.get(RECIPIENT).locale()).isEqualTo("kk");
    }

    @Test
    void expiredIdempotencyKeysAreReleased() {
        context.givenActiveTemplate(ChannelType.EMAIL, "ru", null);
        context.sendService.send(command("key-1", Category.TRANSACTIONAL, ChannelType.EMAIL));

        assertThat(context.idempotencyService.releaseExpired()).isEqualTo(0);
        context.clock.advance(Duration.ofHours(25));
        assertThat(context.idempotencyService.releaseExpired()).isEqualTo(1);
        assertThat(context.notifications.findByIdempotencyKey(CLIENT,
                new kz.company.notification.domain.notification.IdempotencyKey("key-1"))).isEmpty();
    }

    @Test
    void statisticsAreAggregatedPerChannel() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-03T00:00:00Z");
        StatisticsService service = new StatisticsService(criteria -> List.of(
                new StatisticsRow(from, "EMAIL", "SENT", 5),
                new StatisticsRow(from.plus(Duration.ofDays(1)), "EMAIL", "SENT", 3),
                new StatisticsRow(from, "SMS", "FAILED", 1)));

        StatisticsView view = service.statistics(new StatisticsCriteria(CLIENT, from, to, null, null));

        assertThat(view.granularity()).isEqualTo("DAY");
        assertThat(view.totals().get("EMAIL").get("SENT")).isEqualTo(8L);
        assertThat(view.totals().get("SMS").get("FAILED")).isEqualTo(1L);
        assertThat(view.rows()).hasSize(3);
    }

    @Test
    void statisticsPeriodIsValidated() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");

        assertThatThrownBy(() -> new StatisticsCriteria(CLIENT, from, from, null, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new StatisticsCriteria(CLIENT, from, from.plus(Duration.ofDays(100)), null, null))
                .isInstanceOf(DomainValidationException.class);
    }
}
