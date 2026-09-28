package kz.company.notification.domain.policy;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;

import java.time.Duration;
import java.time.Instant;
import java.util.function.DoubleSupplier;

/**
 * Политика повторов: экспоненциальный backoff с jitter и максимум попыток на доставку.
 *
 * <p>Задержка перед повтором после попытки {@code n}: {@code initialDelay * multiplier^(n-1)},
 * не больше {@code maxDelay}, с разбросом ±{@code jitterRatio}. Классификацию ошибок
 * (временная или постоянная) выполняет адаптер канала, возвращая
 * {@code SendResult.RetryableFailure} или {@code SendResult.PermanentFailure}.
 *
 * @param maxAttempts  общее число попыток, включая первую
 * @param jitterRatio  доля случайного разброса, от 0 до 1
 * @param random       источник случайных чисел в диапазоне [0, 1); в тестах фиксируется
 */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay,
                          double jitterRatio, DoubleSupplier random) {

    public RetryPolicy {
        Guard.positive(maxAttempts, "maxAttempts");
        Guard.notNull(initialDelay, "initialDelay");
        Guard.notNull(maxDelay, "maxDelay");
        Guard.notNull(random, "random");
        if (initialDelay.isNegative() || initialDelay.isZero() || maxDelay.compareTo(initialDelay) < 0) {
            throw DomainValidationException.invalidValue("retryDelay", "0 < initialDelay <= maxDelay required");
        }
        if (multiplier < 1.0) {
            throw DomainValidationException.invalidValue("multiplier", "must be >= 1");
        }
        if (jitterRatio < 0.0 || jitterRatio > 1.0) {
            throw DomainValidationException.invalidValue("jitterRatio", "must be between 0 and 1");
        }
    }

    public boolean hasAttemptsLeft(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    /** Решение после неудачной временной попытки номер {@code attemptsMade}. */
    public RetryDecision decide(int attemptsMade, Instant now) {
        if (!hasAttemptsLeft(attemptsMade)) {
            return new RetryDecision.GiveUp();
        }
        return new RetryDecision.RetryAt(now.plus(delayAfterAttempt(attemptsMade)));
    }

    /** Задержка перед следующей попыткой после попытки номер {@code attemptNumber} (с 1). */
    public Duration delayAfterAttempt(int attemptNumber) {
        int exponent = Math.max(0, attemptNumber - 1);
        double baseMillis = initialDelay.toMillis() * Math.pow(multiplier, exponent);
        double cappedMillis = Math.min(baseMillis, maxDelay.toMillis());
        double jitterFactor = 1.0 + jitterRatio * (2.0 * random.getAsDouble() - 1.0);
        long delayMillis = Math.round(Math.min(cappedMillis * jitterFactor, maxDelay.toMillis()));
        return Duration.ofMillis(Math.max(1, delayMillis));
    }
}
