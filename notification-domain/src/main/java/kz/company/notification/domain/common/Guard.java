package kz.company.notification.domain.common;

import java.util.Collection;

/** Проверки инвариантов для конструкторов value objects и агрегатов. */
public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw DomainValidationException.invalidValue(field, "must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw DomainValidationException.invalidValue(field, "must not be blank");
        }
        return value;
    }

    public static String maxLength(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw DomainValidationException.invalidValue(field, "must be at most " + maxLength + " characters");
        }
        return value;
    }

    public static <C extends Collection<?>> C notEmpty(C value, String field) {
        if (value == null || value.isEmpty()) {
            throw DomainValidationException.invalidValue(field, "must not be empty");
        }
        return value;
    }

    public static int positive(int value, String field) {
        if (value <= 0) {
            throw DomainValidationException.invalidValue(field, "must be positive");
        }
        return value;
    }
}
