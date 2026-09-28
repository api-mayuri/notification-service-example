package kz.company.notification.infrastructure.out.channel.push;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.DeviceToken;
import kz.company.notification.domain.notification.ContactType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

/** Канал PUSH: проверяет адрес и передаёт текст сообщения {@link PushProvider}. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.push", name = "enabled", havingValue = "true")
public class PushChannelSender implements ChannelSender {

    private final PushProvider provider;

    public PushChannelSender(PushProvider provider) {
        this.provider = provider;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.PUSH;
    }

    @Override
    public ContactType contactType() {
        return ContactType.DEVICE_TOKEN;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return new DeviceToken(rawAddress);
    }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.push.max-concurrency:50}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof DeviceToken to)) {
            return new SendResult.PermanentFailure("Delivery has no valid PUSH address");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        return provider.send(new PushMessage(delivery.id().value(), to, message.subject(), message.plainText()));
    }
}
