package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.TemplateContent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static kz.company.notification.application.ApplicationTestContext.ORDER_SHIPPED;
import static kz.company.notification.application.ApplicationTestContext.command;
import static org.assertj.core.api.Assertions.assertThat;

class CommandFingerprintTest {

    @Test
    void ignoresOrderOfChannelsAndVariables() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("nested", Map.of("x", List.of(1, 2)));
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("nested", Map.of("x", List.of(1, 2)));
        second.put("a", 1);

        String one = CommandFingerprint.of(command("k", Category.MARKETING, new TemplateContent(ORDER_SHIPPED, null, first),
                ApplicationTestContext.contacts(), ChannelType.EMAIL, ChannelType.SMS));
        String two = CommandFingerprint.of(command("other-key", Category.MARKETING,
                new TemplateContent(ORDER_SHIPPED, null, second), ApplicationTestContext.contacts(),
                ChannelType.SMS, ChannelType.EMAIL));

        assertThat(one).isEqualTo(two);
    }

    @Test
    void differentContentGivesDifferentFingerprint() {
        SendNotificationCommand template = command("k", Category.MARKETING, ChannelType.EMAIL);
        SendNotificationCommand raw = command("k", Category.MARKETING, new RawContent("s", "t", null),
                ApplicationTestContext.contacts(), ChannelType.EMAIL);

        assertThat(CommandFingerprint.of(template)).isNotEqualTo(CommandFingerprint.of(raw));
        assertThat(CommandFingerprint.of(template)).hasSize(64);
    }
}
