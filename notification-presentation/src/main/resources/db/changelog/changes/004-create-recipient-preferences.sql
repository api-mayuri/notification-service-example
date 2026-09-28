--liquibase formatted sql

--changeset notification-service:004-create-recipient-preferences
--comment: Recipient preferences
CREATE TABLE recipient_preferences (
    recipient_id            VARCHAR(100) NOT NULL PRIMARY KEY,
    unsubscribed_channels   VARCHAR(500) NOT NULL DEFAULT '',
    unsubscribed_categories VARCHAR(200) NOT NULL DEFAULT '',
    quiet_hours_start       TIME,
    quiet_hours_end         TIME,
    time_zone               VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    locale                  VARCHAR(35),
    updated_at              TIMESTAMP WITH TIME ZONE,
    version                 BIGINT       NOT NULL
);
--rollback DROP TABLE recipient_preferences;
