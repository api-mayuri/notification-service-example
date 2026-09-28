package kz.company.notification.infrastructure.in.rest;

import kz.company.notification.domain.common.DomainErrorKind;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Соответствие категорий доменных ошибок кодам HTTP (RFC 9110). */
public final class ProblemStatus {

    private static final HttpStatusCode UNPROCESSABLE_CONTENT = HttpStatusCode.valueOf(422);

    private ProblemStatus() {
    }

    public static HttpStatusCode forKind(DomainErrorKind kind) {
        return switch (kind) {
            case VALIDATION -> UNPROCESSABLE_CONTENT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
    }

    public static HttpStatusCode forKind(String kind) {
        return forKind(DomainErrorKind.valueOf(kind));
    }
}
