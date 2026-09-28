package kz.company.notification.infrastructure.out.channel.push;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка PUSH: пишет в лог, что было бы отправлено (адрес замаскирован), и возвращает успех
 * с флагом {@code simulated}. По настройке {@code stub.*} случайно возвращает ошибки.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.push", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingPushProvider implements PushProvider {

    private final PushChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingPushProvider(PushChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(PushMessage message) {
        return stub.simulate("PUSH", message.deliveryId(), message.token().masked(), message.body(), properties.stub());
    }
}
