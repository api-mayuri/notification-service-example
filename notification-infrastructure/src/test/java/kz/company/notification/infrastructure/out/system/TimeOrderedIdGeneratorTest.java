package kz.company.notification.infrastructure.out.system;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimeOrderedIdGeneratorTest {

    @Test
    void generatesVersion7UuidsOrderedByTime() {
        UUID earlier = new TimeOrderedIdGenerator(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)).newId();
        UUID later = new TimeOrderedIdGenerator(Clock.fixed(Instant.parse("2026-01-01T00:00:01Z"), ZoneOffset.UTC)).newId();

        assertThat(earlier.version()).isEqualTo(7);
        assertThat(earlier.variant()).isEqualTo(2);
        assertThat(earlier.getMostSignificantBits() >>> 16).isEqualTo(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli());
        assertThat(Long.compareUnsigned(earlier.getMostSignificantBits(), later.getMostSignificantBits())).isNegative();
    }
}
