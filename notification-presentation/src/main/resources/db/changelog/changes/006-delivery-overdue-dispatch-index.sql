--liquibase formatted sql

--changeset notification-service:006-delivery-overdue-dispatch-index
--comment: Overdue dispatch recovery also covers PENDING deliveries whose message was lost
DROP INDEX ix_delivery_retry_dispatched;
CREATE INDEX ix_delivery_overdue_dispatched ON delivery (next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY_SCHEDULED') AND dispatched_at IS NOT NULL;
--rollback DROP INDEX ix_delivery_overdue_dispatched;
--rollback CREATE INDEX ix_delivery_retry_dispatched ON delivery (next_attempt_at) WHERE status = 'RETRY_SCHEDULED' AND dispatched_at IS NOT NULL;
