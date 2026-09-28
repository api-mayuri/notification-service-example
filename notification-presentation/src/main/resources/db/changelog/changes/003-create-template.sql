--liquibase formatted sql

--changeset notification-service:003-create-template
--comment: Templates (key + channel + locale) and revisions
CREATE TABLE template (
    id                           UUID         NOT NULL PRIMARY KEY,
    template_key                 VARCHAR(100) NOT NULL,
    channel                      VARCHAR(32)  NOT NULL,
    locale                       VARCHAR(35)  NOT NULL,
    deduplication_window_seconds BIGINT,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    version                      BIGINT       NOT NULL,
    CONSTRAINT uq_template_key_channel_locale UNIQUE (template_key, channel, locale)
);
CREATE TABLE template_revision (
    template_id        UUID          NOT NULL REFERENCES template (id) ON DELETE CASCADE,
    revision_number    INTEGER       NOT NULL,
    subject            TEXT,
    body               TEXT          NOT NULL,
    text_body          TEXT,
    required_variables VARCHAR(2000) NOT NULL DEFAULT '',
    status             VARCHAR(16)   NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_template_revision UNIQUE (template_id, revision_number),
    CONSTRAINT ck_template_revision_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);
CREATE UNIQUE INDEX uq_template_single_active ON template_revision (template_id) WHERE status = 'ACTIVE';
--rollback DROP TABLE template_revision;
--rollback DROP TABLE template;
