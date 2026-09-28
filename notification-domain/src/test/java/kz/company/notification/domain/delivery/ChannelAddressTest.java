package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelAddressTest {

    @Test
    void emailIsValidatedAndMasked() {
        EmailAddress email = new EmailAddress(" john.smith@example.com ");

        assertThat(email.value()).isEqualTo("john.smith@example.com");
        assertThat(email.masked()).isEqualTo("j***h@example.com");
        assertThatThrownBy(() -> new EmailAddress("not-an-email")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new EmailAddress("a@b")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void phoneMustBeE164() {
        PhoneNumber phone = new PhoneNumber("+77011234567");

        assertThat(phone.masked()).isEqualTo("+77***567");
        assertThatThrownBy(() -> new PhoneNumber("87011234567")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new PhoneNumber("+0123456789")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void telegramChatIdAcceptsNumbersAndUsernames() {
        assertThat(new TelegramChatId("-1001234567890").value()).isEqualTo("-1001234567890");
        assertThat(new TelegramChatId("@company_news").masked()).isEqualTo("@c***ws");
        assertThatThrownBy(() -> new TelegramChatId("abc")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void deviceTokenHasMinimumLength() {
        assertThat(new DeviceToken("abcdefghijklmnop1234").masked()).isEqualTo("abcd***1234");
        assertThatThrownBy(() -> new DeviceToken("short")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void webhookUrlMustBeAbsoluteHttpWithoutCredentials() {
        WebhookUrl url = new WebhookUrl("https://hooks.example.com/path?token=secret");

        assertThat(url.isHttps()).isTrue();
        assertThat(url.masked()).isEqualTo("https://hooks.example.com/***");
        assertThat(new WebhookUrl("http://localhost:8089/hook").isHttps()).isFalse();
        assertThatThrownBy(() -> new WebhookUrl("ftp://example.com")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new WebhookUrl("https://user:pass@example.com")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new WebhookUrl("not a url")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new WebhookUrl(" ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void channelTypeIsCaseInsensitive() {
        assertThat(ChannelType.of("email")).isEqualTo(ChannelType.EMAIL);
        assertThat(ChannelType.EMAIL.lowerCaseCode()).isEqualTo("email");
        assertThatThrownBy(() -> ChannelType.of("e-mail")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> ChannelType.of(null)).isInstanceOf(DomainValidationException.class);
    }
}
