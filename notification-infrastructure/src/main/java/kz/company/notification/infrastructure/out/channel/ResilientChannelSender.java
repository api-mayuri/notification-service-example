package kz.company.notification.infrastructure.out.channel;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import kz.company.notification.application.port.out.ChannelSender;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.ContactType;

import java.util.concurrent.TimeUnit;

/**
 * Декоратор отправителя: circuit breaker (Resilience4j) и метрики Micrometer на канал.
 * Лимит параллелизма задаётся на самом отправителе аннотацией {@code @ConcurrencyLimit}.
 *
 * <p>Circuit breaker считает сбоями только временные ошибки и исключения: постоянная ошибка
 * (неверный адрес) говорит о данных, а не о здоровье провайдера. Пока breaker открыт,
 * отправка не выполняется и возвращается временная ошибка — доставка уйдёт в повтор.
 */
public final class ResilientChannelSender implements ChannelSender {

    static final String METRIC_NAME = "notification.channel.send";

    private final ChannelSender delegate;
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;

    public ResilientChannelSender(ChannelSender delegate, CircuitBreakerRegistry circuitBreakers,
                                  MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreakers.circuitBreaker("channel-" + delegate.channel().lowerCaseCode(), "channels");
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ChannelType channel() {
        return delegate.channel();
    }

    @Override
    public ContactType contactType() {
        return delegate.contactType();
    }

    @Override
    public ChannelAddress parseAddress(String rawAddress) {
        return delegate.parseAddress(rawAddress);
    }

    @Override
    public SendResult send(Delivery delivery) {
        if (!circuitBreaker.tryAcquirePermission()) {
            record("circuit_open", 0);
            return new SendResult.RetryableFailure("Circuit breaker for channel " + channel() + " is open");
        }
        long start = System.nanoTime();
        try {
            SendResult result = delegate.send(delivery);
            long duration = System.nanoTime() - start;
            switch (result) {
                case SendResult.Success success -> {
                    circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS);
                    record(success.simulated() ? "simulated" : "success", duration);
                }
                case SendResult.PermanentFailure failure -> {
                    circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS);
                    record("permanent_failure", duration);
                }
                case SendResult.RetryableFailure failure -> {
                    circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, new ProviderTemporaryFailure(failure.reason()));
                    record("retryable_failure", duration);
                }
            }
            return result;
        } catch (RuntimeException exception) {
            long duration = System.nanoTime() - start;
            circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, exception);
            record("error", duration);
            throw exception;
        }
    }

    private void record(String outcome, long durationNanos) {
        Timer.builder(METRIC_NAME)
                .description("Отправка сообщения провайдеру канала")
                .tag("channel", channel().lowerCaseCode())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** Маркер временной ошибки провайдера для статистики circuit breaker. */
    static final class ProviderTemporaryFailure extends RuntimeException {
        ProviderTemporaryFailure(String message) {
            super(message, null, false, false);
        }
    }
}
