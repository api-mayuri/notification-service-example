package kz.company.notification.infrastructure.out.channel.sms;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка SMS: пишет в лог, что было бы отправлено (адрес замаскирован), и возвращает успех
 * с флагом {@code simulated}. По настройке {@code stub.*} случайно возвращает ошибки.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.sms", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingSmsProvider implements SmsProvider {

    private final SmsChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingSmsProvider(SmsChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(SmsMessage message) {
        return stub.simulate("SMS", message.deliveryId(), message.to().masked(), message.text(), properties.stub());
    }
}
