--liquibase formatted sql

--changeset notification-service:002-create-delivery
--comment: Per-channel delivery records
CREATE TABLE delivery (
    id                  UUID          NOT NULL PRIMARY KEY,
    notification_id     UUID          NOT NULL REFERENCES notification (id),
    recipient_id        VARCHAR(100)  NOT NULL,
    channel             VARCHAR(32)   NOT NULL,
    address             VARCHAR(4096),
    subject             TEXT,
    text_body           TEXT,
    html_body           TEXT,
    template_key        VARCHAR(100),
    template_version    INTEGER,
    template_locale     VARCHAR(35),
    category            VARCHAR(20)   NOT NULL,
    priority            VARCHAR(10)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    attempts            INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMP WITH TIME ZONE,
    dispatched_at       TIMESTAMP WITH TIME ZONE,
    last_error          VARCHAR(1000),
    provider_message_id VARCHAR(255),
    simulated           BOOLEAN       NOT NULL DEFAULT FALSE,
    skip_reason         VARCHAR(32),
    sent_at             TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT        NOT NULL,
    CONSTRAINT uq_delivery_notification_channel UNIQUE (notification_id, channel)
);
CREATE INDEX ix_delivery_due ON delivery (next_attempt_at)
    WHERE status IN ('PENDING', 'RETRY_SCHEDULED') AND dispatched_at IS NULL;
CREATE INDEX ix_delivery_sending ON delivery (updated_at) WHERE status = 'SENDING';
CREATE INDEX ix_delivery_retry_dispatched ON delivery (next_attempt_at)
    WHERE status = 'RETRY_SCHEDULED' AND dispatched_at IS NOT NULL;
CREATE INDEX ix_delivery_rate ON delivery (recipient_id, channel, sent_at) WHERE sent_at IS NOT NULL;
CREATE INDEX ix_delivery_created ON delivery (created_at);
--rollback DROP TABLE delivery;
