package kz.company.notification.infrastructure.messaging;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

/**
 * Сериализатор значений producer'а: {@link ExternalMessage} — JSON из {@code payload};
 * строки и байты (пересылка в retry-топики и DLT) — как есть.
 */
public class ExternalMessageSerializer implements Serializer<Object> {

    @Override
    public byte[] serialize(String topic, Object data) {
        return switch (data) {
            case null -> null;
            case ExternalMessage message -> message.payload().getBytes(StandardCharsets.UTF_8);
            case byte[] bytes -> bytes;
            case String text -> text.getBytes(StandardCharsets.UTF_8);
            default -> throw new SerializationException("Unsupported Kafka value type " + data.getClass().getName());
        };
    }
}
