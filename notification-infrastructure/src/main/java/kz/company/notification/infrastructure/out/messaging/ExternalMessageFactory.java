package kz.company.notification.infrastructure.out.messaging;

import kz.company.notification.infrastructure.messaging.ExternalMessage;
import kz.company.notification.infrastructure.messaging.NotificationKafkaProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/** Сборка {@link ExternalMessage}: уникальный {@code ce_id}, источник сервиса, payload в JSON. */
@Component
public class ExternalMessageFactory {

    private final JsonMapper jsonMapper;
    private final String source;

    public ExternalMessageFactory(JsonMapper jsonMapper, NotificationKafkaProperties properties) {
        this.jsonMapper = jsonMapper;
        this.source = properties.source();
    }

    public ExternalMessage create(String topic, String key, String type, String subject, Instant time, Object payload) {
        return new ExternalMessage(topic, key, UUID.randomUUID().toString(), source, type, subject, time,
                jsonMapper.writeValueAsString(payload));
    }
}
