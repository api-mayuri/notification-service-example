--liquibase formatted sql

--changeset notification-service:001-create-notification
--comment: Accepted notifications
CREATE TABLE notification (
    id                  UUID         NOT NULL PRIMARY KEY,
    client_id           VARCHAR(200) NOT NULL,
    idempotency_key     VARCHAR(255),
    request_fingerprint VARCHAR(64),
    recipient_id        VARCHAR(100) NOT NULL,
    content_type        VARCHAR(16)  NOT NULL,
    template_key        VARCHAR(100),
    template_locale     VARCHAR(35),
    template_variables  TEXT,
    raw_subject         TEXT,
    raw_text            TEXT,
    raw_html            TEXT,
    channels            VARCHAR(500) NOT NULL,
    category            VARCHAR(20)  NOT NULL,
    priority            VARCHAR(10)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    send_at             TIMESTAMP WITH TIME ZONE,
    deduplication_key   VARCHAR(64),
    metadata            TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT       NOT NULL,
    CONSTRAINT ck_notification_content_type CHECK (content_type IN ('TEMPLATE', 'RAW'))
);
CREATE UNIQUE INDEX uq_notification_client_idempotency_key
    ON notification (client_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_notification_client_created ON notification (client_id, created_at DESC);
CREATE INDEX ix_notification_recipient_created ON notification (recipient_id, created_at DESC);
CREATE INDEX ix_notification_deduplication ON notification (deduplication_key, created_at DESC)
    WHERE deduplication_key IS NOT NULL;
--rollback DROP TABLE notification;
