package kz.company.notification.infrastructure.out.channel.whatsapp;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.PhoneNumber;
import kz.company.notification.domain.notification.ContactType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

/** Канал WHATSAPP: проверяет адрес и передаёт текст сообщения {@link WhatsAppProvider}. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.whatsapp", name = "enabled", havingValue = "true")
public class WhatsAppChannelSender implements ChannelSender {

    private final WhatsAppProvider provider;

    public WhatsAppChannelSender(WhatsAppProvider provider) {
        this.provider = provider;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.WHATSAPP;
    }

    @Override
    public ContactType contactType() {
        return ContactType.PHONE;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return new PhoneNumber(rawAddress);
    }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.whatsapp.max-concurrency:10}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof PhoneNumber to)) {
            return new SendResult.PermanentFailure("Delivery has no valid WHATSAPP address");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        return provider.send(new WhatsAppMessage(delivery.id().value(), to, message.plainText()));
    }
}
