package kz.company.notification.application.port.in;

import kz.company.notification.domain.common.DomainValidationException;

/** Номер страницы (с 0) и размер страницы (1–200). */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 200;

    public PageQuery {
        if (page < 0) {
            throw DomainValidationException.invalidValue("page", "must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw DomainValidationException.invalidValue("size", "must be between 1 and " + MAX_SIZE);
        }
    }
}
