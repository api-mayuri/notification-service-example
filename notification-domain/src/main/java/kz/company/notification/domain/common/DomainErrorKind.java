package kz.company.notification.domain.common;

/**
 * Semantic category of a domain error. Inbound adapters map it to a protocol response
 * (e.g. HTTP status) without knowing specific exception classes.
 */
public enum DomainErrorKind {
    /** Business rule violated or input fails invariants. */
    VALIDATION,
    /** Requested object does not exist. */
    NOT_FOUND,
    /** Operation conflicts with current object state. */
    CONFLICT,
    /** Caller lacks permission for the operation. */
    FORBIDDEN
}
