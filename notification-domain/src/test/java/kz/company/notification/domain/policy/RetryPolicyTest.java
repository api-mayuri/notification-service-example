package kz.company.notification.domain.policy;

import kz.company.notification.domain.common.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryPolicyTest {

    @Test
    void delayGrowsExponentiallyUpToMaximum() {
        RetryPolicy policy = new RetryPolicy(10, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), 0.0, () -> 0.9);

        assertThat(policy.delayAfterAttempt(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.delayAfterAttempt(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.delayAfterAttempt(3)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.delayAfterAttempt(4)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void jitterSpreadsDelayWithinRatio() {
        RetryPolicy low = new RetryPolicy(3, Duration.ofSeconds(10), 2.0, Duration.ofMinutes(5), 0.2, () -> 0.0);
        RetryPolicy high = new RetryPolicy(3, Duration.ofSeconds(10), 2.0, Duration.ofMinutes(5), 0.2, () -> 0.999_999);

        assertThat(low.delayAfterAttempt(1)).isEqualTo(Duration.ofSeconds(8));
        assertThat(high.delayAfterAttempt(1)).isEqualTo(Duration.ofSeconds(12));
    }

    @Test
    void decideGivesUpAfterMaxAttempts() {
        RetryPolicy policy = new RetryPolicy(2, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5), 0.0, () -> 0.5);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(policy.decide(1, now)).isEqualTo(new RetryDecision.RetryAt(now.plusSeconds(1)));
        assertThat(policy.decide(2, now)).isInstanceOf(RetryDecision.GiveUp.class);
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThatThrownBy(() -> new RetryPolicy(0, Duration.ofSeconds(1), 2, Duration.ofSeconds(1), 0, () -> 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RetryPolicy(1, Duration.ofSeconds(2), 2, Duration.ofSeconds(1), 0, () -> 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RetryPolicy(1, Duration.ofSeconds(1), 0.5, Duration.ofSeconds(1), 0, () -> 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new RetryPolicy(1, Duration.ofSeconds(1), 2, Duration.ofSeconds(1), 1.5, () -> 0))
                .isInstanceOf(DomainValidationException.class);
    }
}
