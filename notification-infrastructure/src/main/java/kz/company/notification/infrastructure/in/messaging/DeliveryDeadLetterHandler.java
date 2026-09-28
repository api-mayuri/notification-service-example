package kz.company.notification.infrastructure.in.messaging;

import kz.company.notification.application.port.in.ProcessDeliveryUseCase;
import kz.company.notification.domain.delivery.DeliveryId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Обработчик parking lot: запись попала в DLT топика доставок. Доставка переводится в FAILED
 * (если ещё не там), запись остаётся в DLT для разбора и ручного повтора (см. README).
 */
@Component(DeliveryDeadLetterHandler.BEAN_NAME)
public class DeliveryDeadLetterHandler {

    public static final String BEAN_NAME = "deliveryDeadLetterHandler";
    public static final String METHOD_NAME = "handle";

    private static final Logger log = LoggerFactory.getLogger(DeliveryDeadLetterHandler.class);

    private final ProcessDeliveryUseCase processDeliveryUseCase;
    private final JsonMapper jsonMapper;

    public DeliveryDeadLetterHandler(ProcessDeliveryUseCase processDeliveryUseCase, JsonMapper jsonMapper) {
        this.processDeliveryUseCase = processDeliveryUseCase;
        this.jsonMapper = jsonMapper;
    }

    public void handle(ConsumerRecord<String, String> record) {
        String reason = headerText(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        DeliveryId deliveryId;
        try {
            deliveryId = ChannelDeliveryListener.deliveryId(jsonMapper, record);
        } catch (InvalidMessageException invalid) {
            log.error("Unreadable record in dead letter topic {} partition={} offset={}: {}", record.topic(),
                    record.partition(), record.offset(), invalid.getMessage());
            return;
        }
        boolean parked = processDeliveryUseCase.park(deliveryId, "Moved to dead letter topic: " + reason);
        log.warn("Delivery {} is in dead letter topic {} (status changed: {}), reason: {}", deliveryId, record.topic(),
                parked, reason);
    }

    private static String headerText(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null || header.value() == null ? "unknown" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
