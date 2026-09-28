package kz.company.notification.infrastructure.out.channel.telegram;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка TELEGRAM: пишет в лог, что было бы отправлено (адрес замаскирован), и возвращает успех
 * с флагом {@code simulated}. По настройке {@code stub.*} случайно возвращает ошибки.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.telegram", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingTelegramProvider implements TelegramProvider {

    private final TelegramChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingTelegramProvider(TelegramChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(TelegramMessage message) {
        return stub.simulate("TELEGRAM", message.deliveryId(), message.chatId().masked(), message.text(), properties.stub());
    }
}
