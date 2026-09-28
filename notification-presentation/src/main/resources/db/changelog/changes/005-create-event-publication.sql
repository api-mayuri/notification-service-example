--liquibase formatted sql

--changeset notification-service:005-create-event-publication
--comment: Spring Modulith event publication log (transactional outbox)
CREATE TABLE event_publication (
    id                     UUID NOT NULL PRIMARY KEY,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
--rollback DROP TABLE event_publication;
