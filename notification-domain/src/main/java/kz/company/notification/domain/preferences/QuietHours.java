package kz.company.notification.domain.preferences;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;

import java.time.LocalTime;

/**
 * Тихие часы в местном времени получателя: {@code [start, end)}. Интервал может переходить
 * через полночь, например 22:00–08:00.
 */
public record QuietHours(LocalTime start, LocalTime end) {

    public QuietHours {
        Guard.notNull(start, "quietHours.start");
        Guard.notNull(end, "quietHours.end");
        if (start.equals(end)) {
            throw DomainValidationException.invalidValue("quietHours", "start and end must differ");
        }
    }

    public boolean crossesMidnight() {
        return start.isAfter(end);
    }

    public boolean contains(LocalTime time) {
        if (crossesMidnight()) {
            return !time.isBefore(start) || time.isBefore(end);
        }
        return !time.isBefore(start) && time.isBefore(end);
    }
}
