package kz.company.notification.infrastructure.out.channel.whatsapp;

import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubProviderSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка WHATSAPP: пишет в лог, что было бы отправлено (адрес замаскирован), и возвращает успех
 * с флагом {@code simulated}. По настройке {@code stub.*} случайно возвращает ошибки.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.whatsapp", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingWhatsAppProvider implements WhatsAppProvider {

    private final WhatsAppChannelProperties properties;
    private final StubProviderSupport stub = StubProviderSupport.withSystemRandom();

    public LoggingWhatsAppProvider(WhatsAppChannelProperties properties) {
        this.properties = properties;
    }

    @Override
    public SendResult send(WhatsAppMessage message) {
        return stub.simulate("WHATSAPP", message.deliveryId(), message.to().masked(), message.text(), properties.stub());
    }
}
