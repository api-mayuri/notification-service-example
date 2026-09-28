package kz.company.notification.domain.common;

/** Нарушен инвариант value object или агрегата. */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String errorCode, String message) {
        super(errorCode, DomainErrorKind.VALIDATION, message);
    }

    public static DomainValidationException invalidValue(String field, String reason) {
        return new DomainValidationException("INVALID_" + toCode(field), field + ": " + reason);
    }

    private static String toCode(String field) {
        return field.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
    }
}
