package kz.company.notification.infrastructure.out.channel.sms;

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

/** Канал SMS: проверяет адрес и передаёт текст сообщения {@link SmsProvider}. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.sms", name = "enabled", havingValue = "true")
public class SmsChannelSender implements ChannelSender {

    private final SmsProvider provider;

    public SmsChannelSender(SmsProvider provider) {
        this.provider = provider;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.SMS;
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
    @ConcurrencyLimit(limitString = "${notification.channels.sms.max-concurrency:10}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof PhoneNumber to)) {
            return new SendResult.PermanentFailure("Delivery has no valid SMS address");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        return provider.send(new SmsMessage(delivery.id().value(), to, message.plainText()));
    }
}
