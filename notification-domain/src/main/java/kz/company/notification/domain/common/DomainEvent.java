package kz.company.notification.domain.common;

import java.time.Instant;

/**
 * Domain event: a fact that has already occurred in the model.
 *
 * <p>Events are immutable (records) and contain only simple data so they can be
 * stored in the outbox and restored without knowledge of domain objects.
 */
public interface DomainEvent {

    /** When the event occurred. */
    Instant occurredAt();
}
