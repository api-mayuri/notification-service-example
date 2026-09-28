package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ContactType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Канал email: превращает доставку в письмо и передаёт {@link EmailProvider}. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.email", name = "enabled", havingValue = "true")
public class EmailChannelSender implements ChannelSender {

    private final EmailProvider provider;
    private final EmailChannelProperties properties;

    public EmailChannelSender(EmailProvider provider, EmailChannelProperties properties) {
        this.provider = provider;
        this.properties = properties;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.EMAIL;
    }

    @Override
    public ContactType contactType() {
        return ContactType.EMAIL;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return new EmailAddress(rawAddress);
    }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.email.max-concurrency:20}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof EmailAddress to)) {
            return new SendResult.PermanentFailure("Delivery has no valid email address");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        String unsubscribeUrl = delivery.category() == Category.MARKETING && properties.unsubscribeUrlTemplate() != null
                && !properties.unsubscribeUrlTemplate().isBlank()
                ? properties.unsubscribeUrlTemplate()
                        .replace("{recipientId}", URLEncoder.encode(delivery.recipientId().value(), StandardCharsets.UTF_8))
                        .replace("{deliveryId}", delivery.id().toString())
                : null;
        return provider.send(new EmailMessage(delivery.id().value(), delivery.recipientId().value(), to,
                message.subject(), message.plainText(), message.optionalHtmlBody().orElse(null), unsubscribeUrl));
    }
}
