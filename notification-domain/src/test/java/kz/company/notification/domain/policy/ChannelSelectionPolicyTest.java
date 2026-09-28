package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.PhoneNumber;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ContactType;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.RecipientPreferences;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelSelectionPolicyTest {

    private final ChannelSelectionPolicy policy = new ChannelSelectionPolicy();

    private static final ChannelAddressLookup LOOKUP = (channel, contacts) -> {
        if (channel.equals(ChannelType.EMAIL)) {
            return contacts.find(ContactType.EMAIL).map(value -> (ChannelAddress) new EmailAddress(value));
        }
        if (channel.equals(ChannelType.SMS)) {
            return contacts.find(ContactType.PHONE).map(value -> (ChannelAddress) new PhoneNumber(value));
        }
        return Optional.empty();
    };

    private static final RecipientContacts CONTACTS = new RecipientContacts(Map.of(
            ContactType.EMAIL, "user@example.com", ContactType.PHONE, "+77011234567"));

    private static Set<ChannelType> channels(ChannelType... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private static RecipientPreferences unsubscribedFromSmsAndMarketing() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        preferences.apply(new PreferencesChange(Set.of(ChannelType.SMS), Set.of(Category.MARKETING), null, false,
                null, null, false), Instant.EPOCH);
        return preferences;
    }

    @Test
    void selectsChannelsWithContacts() {
        ChannelSelection selection = policy.select(channels(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH),
                Category.MARKETING, RecipientPreferences.defaults(new RecipientId("user-1")), CONTACTS, LOOKUP);

        assertThat(selection.selected()).hasSize(2);
        assertThat(selection.skipped()).containsExactly(new ChannelSelection.Skipped(ChannelType.PUSH, SkipReason.NO_CONTACT));
    }

    @Test
    void marketingRespectsUnsubscribes() {
        ChannelSelection selection = policy.select(channels(ChannelType.EMAIL, ChannelType.SMS), Category.MARKETING,
                unsubscribedFromSmsAndMarketing(), CONTACTS, LOOKUP);

        assertThat(selection.selected()).isEmpty();
        assertThat(selection.skipped()).containsExactly(
                new ChannelSelection.Skipped(ChannelType.EMAIL, SkipReason.UNSUBSCRIBED_CATEGORY),
                new ChannelSelection.Skipped(ChannelType.SMS, SkipReason.UNSUBSCRIBED_CATEGORY));
    }

    @Test
    void channelUnsubscribeSkipsOnlyThatChannel() {
        RecipientPreferences preferences = RecipientPreferences.defaults(new RecipientId("user-1"));
        preferences.apply(new PreferencesChange(Set.of(ChannelType.SMS), null, null, false, null, null, false),
                Instant.EPOCH);

        ChannelSelection selection = policy.select(channels(ChannelType.EMAIL, ChannelType.SMS), Category.MARKETING,
                preferences, CONTACTS, LOOKUP);

        assertThat(selection.selected()).hasSize(1);
        assertThat(selection.skipped()).containsExactly(
                new ChannelSelection.Skipped(ChannelType.SMS, SkipReason.UNSUBSCRIBED_CHANNEL));
    }

    @Test
    void transactionalIgnoresUnsubscribes() {
        ChannelSelection selection = policy.select(channels(ChannelType.EMAIL, ChannelType.SMS), Category.TRANSACTIONAL,
                unsubscribedFromSmsAndMarketing(), CONTACTS, LOOKUP);

        assertThat(selection.selected()).hasSize(2);
        assertThat(selection.skipped()).isEmpty();
    }

    @Test
    void failsWhenNoChannelHasContact() {
        assertThatThrownBy(() -> policy.select(channels(ChannelType.PUSH), Category.TRANSACTIONAL,
                RecipientPreferences.defaults(new RecipientId("user-1")), RecipientContacts.NONE, LOOKUP))
                .isInstanceOf(NoDeliverableChannelException.class);
    }
}
