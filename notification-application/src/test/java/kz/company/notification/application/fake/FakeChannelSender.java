package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.ContactType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/** Отправитель со сценарием ответов; по умолчанию — успех. */
public final class FakeChannelSender implements ChannelSender {

    private final ChannelType channel;
    private final ContactType contactType;
    private final Function<String, ChannelAddress> parser;
    private final Deque<Object> scriptedResults = new ArrayDeque<>();
    private final List<Delivery> sent = new ArrayList<>();

    public FakeChannelSender(ChannelType channel, ContactType contactType, Function<String, ChannelAddress> parser) {
        this.channel = channel;
        this.contactType = contactType;
        this.parser = parser;
    }

    /**
     * Следующий ответ: {@link SendResult}, {@link RuntimeException} (будет брошено) или
     * {@link java.util.function.Supplier} — вызывается во время отправки.
     */
    public FakeChannelSender willReturn(Object result) {
        scriptedResults.add(result);
        return this;
    }

    public List<Delivery> sent() {
        return sent;
    }

    @Override
    public ChannelType channel() {
        return channel;
    }

    @Override
    public ContactType contactType() {
        return contactType;
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return parser.apply(rawAddress);
    }

    @Override
    public SendResult send(Delivery delivery) {
        sent.add(delivery);
        Object next = scriptedResults.isEmpty() ? SendResult.Success.real("provider-" + sent.size()) : scriptedResults.poll();
        if (next instanceof java.util.function.Supplier<?> supplier) {
            next = supplier.get();
        }
        if (next instanceof RuntimeException exception) {
            throw exception;
        }
        return (SendResult) next;
    }
}
