package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.WebhookUrl;
import kz.company.notification.domain.notification.ContactType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

/** Канал webhook: HTTP-вызов системы получателя. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.webhook", name = "enabled", havingValue = "true")
public class WebhookChannelSender implements ChannelSender {

    private final WebhookProvider provider;

    public WebhookChannelSender(WebhookProvider provider) {
        this.provider = provider;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.WEBHOOK;
    }

    @Override
    public ContactType contactType() {
        return ContactType.WEBHOOK_URL;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return new WebhookUrl(rawAddress);
    }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.webhook.max-concurrency:20}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof WebhookUrl url)) {
            return new SendResult.PermanentFailure("Delivery has no valid webhook URL");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        return provider.send(new WebhookMessage(delivery.id().value(), delivery.notificationId().value(),
                delivery.recipientId().value(), url, message.subject(), message.plainText(),
                message.optionalHtmlBody().orElse(null)));
    }
}
