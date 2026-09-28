package kz.company.notification.infrastructure.out.channel;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.PhoneNumber;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.ContactType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientChannelSenderTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final CircuitBreakerRegistry circuitBreakers = CircuitBreakerRegistry.of(Map.of("channels",
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(4)
                    .minimumNumberOfCalls(4)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofMinutes(1))
                    .build()));

    private static ChannelSender failingSender(AtomicInteger calls) {
        return new ChannelSender() {
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
            public SendResult send(Delivery delivery) {
                calls.incrementAndGet();
                return new SendResult.RetryableFailure("gateway timeout");
            }
        };
    }

    @Test
    void circuitOpensAfterTemporaryFailuresAndStopsCallingProvider() {
        AtomicInteger calls = new AtomicInteger();
        ResilientChannelSender sender = new ResilientChannelSender(failingSender(calls), circuitBreakers, meters);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThat(sender.send(null)).isInstanceOf(SendResult.RetryableFailure.class);
        }
        SendResult whileOpen = sender.send(null);

        assertThat(calls.get()).isEqualTo(4);
        assertThat(((SendResult.RetryableFailure) whileOpen).reason()).contains("Circuit breaker");
        assertThat(meters.find(ResilientChannelSender.METRIC_NAME).tag("outcome", "circuit_open").timer().count())
                .isEqualTo(1);
    }

    @Test
    void registryRejectsDuplicateChannels() {
        AtomicInteger calls = new AtomicInteger();
        ChannelSender sender = failingSender(calls);

        assertThatThrownBy(() -> new SpringChannelSenderRegistry(List.of(sender, sender), circuitBreakers, meters))
                .isInstanceOf(IllegalStateException.class);
        SpringChannelSenderRegistry registry = new SpringChannelSenderRegistry(List.of(sender), circuitBreakers, meters);
        assertThat(registry.enabledChannels()).containsExactly(ChannelType.SMS);
        assertThat(registry.find(ChannelType.EMAIL)).isEmpty();
    }
}
