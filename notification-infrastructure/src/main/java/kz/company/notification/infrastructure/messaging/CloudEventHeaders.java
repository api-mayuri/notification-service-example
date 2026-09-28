package kz.company.notification.infrastructure.messaging;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Заголовки CloudEvents 1.0 Kafka Protocol Binding (binary content mode). */
public final class CloudEventHeaders {

    public static final String ID = "ce_id";
    public static final String SOURCE = "ce_source";
    public static final String TYPE = "ce_type";
    public static final String SPEC_VERSION = "ce_specversion";
    public static final String TIME = "ce_time";
    public static final String SUBJECT = "ce_subject";
    public static final String CONTENT_TYPE = "content-type";
    public static final String SPEC_VERSION_VALUE = "1.0";
    public static final String JSON = "application/json";

    private CloudEventHeaders() {
    }

    /** Заголовки исходящего сообщения. */
    public static Map<String, Object> of(ExternalMessage message) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(ID, message.id());
        headers.put(SOURCE, message.source());
        headers.put(TYPE, message.type());
        headers.put(SPEC_VERSION, SPEC_VERSION_VALUE);
        headers.put(TIME, message.time().toString());
        if (message.subject() != null) {
            headers.put(SUBJECT, message.subject());
        }
        headers.put(CONTENT_TYPE, JSON);
        return headers;
    }

    /** Строковое значение заголовка входящей записи; кавычки JSON-строки снимаются. */
    public static Optional<String> read(Headers headers, String name) {
        Header header = headers.lastHeader(name);
        if (header == null || header.value() == null) {
            return Optional.empty();
        }
        String value = new String(header.value(), StandardCharsets.UTF_8).strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
}
