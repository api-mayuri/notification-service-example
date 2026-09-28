package kz.company.notification.application.exception;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.notification.IdempotencyKey;

/** Ключ идемпотентности уже использован с другим телом запроса (422 по IETF draft). */
public class IdempotencyKeyReusedException extends DomainValidationException {

    public IdempotencyKeyReusedException(IdempotencyKey key) {
        super("IDEMPOTENCY_KEY_REUSED", "Idempotency key '" + key + "' was already used with a different request");
    }
}
