package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Заглушка webhook для тестов и локального запуска ({@code provider=logging}). */
@Component
@ConditionalOnProperty(prefix = "notification.channels.webhook", name = "provider", havingValue = "logging")
public class LoggingWebhookProvider implements WebhookProvider {

    private final WebhookChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingWebhookProvider(WebhookChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(WebhookMessage message) {
        return stub.simulate("WEBHOOK", message.deliveryId(), message.url().masked(), message.text(), properties.stub());
    }
}
