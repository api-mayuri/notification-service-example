package kz.company.notification.infrastructure.out.system;

import kz.company.notification.application.port.out.IdGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * UUID версии 7 (RFC 9562): 48 бит времени в миллисекундах + случайные биты. Идентификаторы
 * растут со временем, поэтому вставки в B-tree индексы PostgreSQL остаются локальными.
 */
@Component
public class TimeOrderedIdGenerator implements IdGenerator {

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TimeOrderedIdGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UUID newId() {
        long timestamp = clock.millis() & 0xFFFF_FFFF_FFFFL;
        long randomA = random.nextLong();
        long randomB = random.nextLong();
        long mostSignificant = (timestamp << 16) | 0x7000L | (randomA & 0x0FFFL);
        long leastSignificant = (randomB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
