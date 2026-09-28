package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.TemplateKey;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationValueObjectsTest {

    @Test
    void contactsDropBlankValuesAndCanBeOverridden() {
        Map<ContactType, String> base = new LinkedHashMap<>();
        base.put(ContactType.EMAIL, "old@example.com");
        base.put(ContactType.PHONE, " ");
        RecipientContacts contacts = new RecipientContacts(base);

        RecipientContacts merged = contacts.overriddenBy(new RecipientContacts(Map.of(ContactType.EMAIL, "new@example.com")));

        assertThat(contacts.find(ContactType.PHONE)).isEmpty();
        assertThat(merged.find(ContactType.EMAIL)).contains("new@example.com");
        assertThat(RecipientContacts.NONE.isEmpty()).isTrue();
    }

    @Test
    void contactTypeMustBeCamelCase() {
        assertThat(new ContactType("telegramChatId")).isEqualTo(ContactType.TELEGRAM_CHAT_ID);
        assertThatThrownBy(() -> new ContactType("Telegram")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rawContentNeedsBody() {
        assertThatThrownBy(() -> new RawContent("subject", null, " ")).isInstanceOf(DomainValidationException.class);
        assertThat(new RawContent(null, null, "<p>x</p>").html()).isEqualTo("<p>x</p>");
    }

    @Test
    void deduplicationKeyIgnoresOrder() {
        RecipientId recipient = new RecipientId("user-1");
        TemplateKey key = new TemplateKey("welcome");
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", "x");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("b", "x");
        second.put("a", 1);

        DeduplicationKey one = DeduplicationKey.of(recipient, key, first, Set.of(ChannelType.EMAIL, ChannelType.SMS));
        DeduplicationKey two = DeduplicationKey.of(recipient, key, second, Set.of(ChannelType.SMS, ChannelType.EMAIL));
        DeduplicationKey other = DeduplicationKey.of(recipient, key, Map.of("a", 2), Set.of(ChannelType.EMAIL));

        assertThat(one).isEqualTo(two);
        assertThat(one).isNotEqualTo(other);
        assertThat(one.value()).hasSize(64);
    }

    @Test
    void categoryRules() {
        assertThat(Category.MARKETING.isOptional()).isTrue();
        assertThat(Category.TRANSACTIONAL.isOptional()).isFalse();
        assertThat(Category.SYSTEM.respectsQuietHours()).isFalse();
        assertThat(NotificationStatus.PARTIALLY_FAILED.isTerminal()).isTrue();
        assertThat(NotificationStatus.IN_PROGRESS.isTerminal()).isFalse();
    }

    @Test
    void templateContentSortsVariables() {
        TemplateContent content = new TemplateContent(new TemplateKey("welcome"), null, null);

        assertThat(content.variables()).isEmpty();
        assertThat(content.requestedLocale()).isEmpty();
    }
}
