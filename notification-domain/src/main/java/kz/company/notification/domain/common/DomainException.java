package kz.company.notification.domain.common;

import java.util.Objects;

/**
 * Базовое доменное исключение с машинно-читаемым кодом ошибки.
 *
 * <p>Код стабилен и входит в публичный контракт API (поле {@code errorCode} в Problem Details).
 */
public class DomainException extends RuntimeException {

    private final String errorCode;
    private final DomainErrorKind kind;

    public DomainException(String errorCode, DomainErrorKind kind, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public String errorCode() {
        return errorCode;
    }

    public DomainErrorKind kind() {
        return kind;
    }
}
