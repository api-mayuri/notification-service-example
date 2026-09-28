package kz.company.notification.domain.policy;

import java.time.Instant;

/** Решение {@link RetryPolicy}: повторить в момент {@code at} или прекратить попытки. */
public sealed interface RetryDecision {

    record RetryAt(Instant at) implements RetryDecision {
    }

    record GiveUp() implements RetryDecision {
    }
}
