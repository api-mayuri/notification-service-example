package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.domain.delivery.ChannelType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FakeChannelSenderRegistry implements ChannelSenderRegistry {

    private final Map<ChannelType, ChannelSender> senders = new LinkedHashMap<>();

    public FakeChannelSenderRegistry register(ChannelSender sender) {
        senders.put(sender.channel(), sender);
        return this;
    }

    public void unregister(ChannelType channel) {
        senders.remove(channel);
    }

    @Override
    public Set<ChannelType> enabledChannels() {
        return Set.copyOf(senders.keySet());
    }

    @Override
    public Optional<ChannelSender> find(ChannelType channel) {
        return Optional.ofNullable(senders.get(channel));
    }
}
