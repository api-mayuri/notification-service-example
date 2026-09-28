package kz.company.notification.infrastructure.out.channel;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.domain.delivery.ChannelType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Реестр отправителей: собирает все бины {@link ChannelSender} (выключенные каналы не
 * регистрируются благодаря {@code @ConditionalOnProperty}) и оборачивает каждый декоратором
 * {@link ResilientChannelSender}: circuit breaker и метрики на канал.
 */
@Component
public class SpringChannelSenderRegistry implements ChannelSenderRegistry {

    private final Map<ChannelType, ChannelSender> senders;

    public SpringChannelSenderRegistry(List<ChannelSender> channelSenders, CircuitBreakerRegistry circuitBreakers,
                                       MeterRegistry meterRegistry) {
        Map<ChannelType, ChannelSender> byChannel = new TreeMap<>();
        for (ChannelSender sender : channelSenders) {
            ChannelSender previous = byChannel.put(sender.channel(),
                    new ResilientChannelSender(sender, circuitBreakers, meterRegistry));
            if (previous != null) {
                throw new IllegalStateException("More than one ChannelSender for channel " + sender.channel());
            }
        }
        this.senders = Collections.unmodifiableMap(byChannel);
    }

    @Override
    public Set<ChannelType> enabledChannels() {
        return senders.keySet();
    }

    @Override
    public Optional<ChannelSender> find(ChannelType channel) {
        return Optional.ofNullable(senders.get(channel));
    }
}
