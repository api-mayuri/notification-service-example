package kz.company.notification.application.exception;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

/**
 * Запрос с тем же ключом идемпотентности обрабатывается параллельно (409 по IETF draft).
 * Бросается хранилищем при нарушении уникальности пары (client_id, key).
 */
public class ConcurrentIdempotentRequestException extends DomainException {

    public ConcurrentIdempotentRequestException() {
        super("IDEMPOTENT_REQUEST_IN_PROGRESS", DomainErrorKind.CONFLICT,
                "A request with the same idempotency key is being processed; retry later");
    }
}
