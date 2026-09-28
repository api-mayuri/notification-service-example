package kz.company.notification.infrastructure.out.channel.telegram;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.delivery.TelegramChatId;
import kz.company.notification.domain.notification.ContactType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;

/** Канал TELEGRAM: проверяет адрес и передаёт текст сообщения {@link TelegramProvider}. */
@Component
@ConditionalOnProperty(prefix = "notification.channels.telegram", name = "enabled", havingValue = "true")
public class TelegramChannelSender implements ChannelSender {

    private final TelegramProvider provider;

    public TelegramChannelSender(TelegramProvider provider) {
        this.provider = provider;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public ContactType contactType() {
        return ContactType.TELEGRAM_CHAT_ID;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return new TelegramChatId(rawAddress);
    }

    @Override
    @ConcurrencyLimit(limitString = "${notification.channels.telegram.max-concurrency:20}")
    public SendResult send(Delivery delivery) {
        if (!(delivery.address().orElse(null) instanceof TelegramChatId to)) {
            return new SendResult.PermanentFailure("Delivery has no valid TELEGRAM address");
        }
        RenderedMessage message = delivery.message().orElseThrow();
        return provider.send(new TelegramMessage(delivery.id().value(), to, message.plainText()));
    }
}
