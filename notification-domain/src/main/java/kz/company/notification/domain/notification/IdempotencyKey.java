package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;

import java.util.regex.Pattern;

/**
 * Ключ идемпотентности (IETF draft «The Idempotency-Key HTTP Header Field»).
 * Уникален в паре с {@link ClientId}.
 */
public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 255;
    private static final Pattern PRINTABLE_ASCII = Pattern.compile("[\\x21-\\x7E]+");

    public IdempotencyKey {
        Guard.notBlank(value, "idempotencyKey");
        Guard.maxLength(value, MAX_LENGTH, "idempotencyKey");
        if (!PRINTABLE_ASCII.matcher(value).matches()) {
            throw DomainValidationException.invalidValue("idempotencyKey", "must contain printable ASCII characters only");
        }
    }

    /** Ключ элемента пакета: ключ пакета + порядковый номер элемента. */
    public IdempotencyKey forBatchItem(int index) {
        return new IdempotencyKey(value + ":" + index);
    }

    @Override
    public String toString() {
        return value;
    }
}
