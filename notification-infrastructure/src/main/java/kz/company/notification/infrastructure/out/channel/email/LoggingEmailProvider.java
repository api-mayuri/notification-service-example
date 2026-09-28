package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Заглушка email для тестов и запуска без SMTP ({@code provider=logging}). */
@Component
@ConditionalOnProperty(prefix = "notification.channels.email", name = "provider", havingValue = "logging")
public class LoggingEmailProvider implements EmailProvider {

    private final EmailChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingEmailProvider(EmailChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(EmailMessage message) {
        return stub.simulate("EMAIL", message.deliveryId(), message.to().masked(), message.subject(), properties.stub());
    }
}
