package kz.company.notification.domain.preferences;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.template.LocaleTag;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static kz.company.notification.domain.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipientPreferencesTest {

    @Test
    void defaultsAllowEverything() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));

        assertThat(preferences.allowsCategory(Category.MARKETING)).isTrue();
        assertThat(preferences.allowsChannel(ChannelType.SMS, Category.MARKETING)).isTrue();
        assertThat(preferences.timeZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(preferences.quietHours()).isEmpty();
        assertThat(preferences.updatedAt()).isEmpty();
    }

    @Test
    void unsubscribesDoNotAffectTransactional() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));

        preferences.apply(new PreferencesChange(Set.of(ChannelType.SMS), Set.of(Category.MARKETING), null, false,
                null, null, false), NOW);

        assertThat(preferences.allowsCategory(Category.MARKETING)).isFalse();
        assertThat(preferences.allowsCategory(Category.TRANSACTIONAL)).isTrue();
        assertThat(preferences.allowsChannel(ChannelType.SMS, Category.MARKETING)).isFalse();
        assertThat(preferences.allowsChannel(ChannelType.SMS, Category.TRANSACTIONAL)).isTrue();
        assertThat(preferences.allowsChannel(ChannelType.SMS, Category.SYSTEM)).isTrue();
    }

    @Test
    void transactionalCategoryCannotBeUnsubscribed() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));

        assertThatThrownBy(() -> preferences.apply(new PreferencesChange(null, Set.of(Category.TRANSACTIONAL), null,
                false, null, null, false), NOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("TRANSACTIONAL");
    }

    @Test
    void partialChangeKeepsOtherFieldsAndClearFlagsWork() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        QuietHours quietHours = new QuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        preferences.apply(PreferencesChange.replaceWith(Set.of(ChannelType.PUSH), Set.of(), quietHours,
                ZoneId.of("Asia/Almaty"), LocaleTag.of("kk")), NOW);

        preferences.apply(new PreferencesChange(null, null, null, false, null, null, true), NOW);

        assertThat(preferences.unsubscribedChannels()).containsExactly(ChannelType.PUSH);
        assertThat(preferences.quietHours()).contains(quietHours);
        assertThat(preferences.locale()).isEmpty();

        preferences.apply(new PreferencesChange(null, null, null, true, null, null, false), NOW);
        assertThat(preferences.quietHours()).isEmpty();
        assertThat(preferences.timeZone()).isEqualTo(ZoneId.of("Asia/Almaty"));
    }

    @Test
    void snapshotRoundTrip() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        preferences.apply(PreferencesChange.replaceWith(null, null, null, null, null), NOW);

        assertThat(RecipientPreferences.restore(preferences.snapshot()).snapshot()).isEqualTo(preferences.snapshot());
    }

    @Test
    void quietHoursContainment() {
        QuietHours overnight = new QuietHours(LocalTime.of(22, 0), LocalTime.of(8, 0));
        QuietHours daytime = new QuietHours(LocalTime.of(13, 0), LocalTime.of(14, 0));

        assertThat(overnight.contains(LocalTime.of(23, 0))).isTrue();
        assertThat(overnight.contains(LocalTime.of(7, 59))).isTrue();
        assertThat(overnight.contains(LocalTime.of(8, 0))).isFalse();
        assertThat(daytime.contains(LocalTime.of(13, 30))).isTrue();
        assertThat(daytime.contains(LocalTime.of(14, 0))).isFalse();
        assertThatThrownBy(() -> new QuietHours(LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(DomainValidationException.class);
    }
}
