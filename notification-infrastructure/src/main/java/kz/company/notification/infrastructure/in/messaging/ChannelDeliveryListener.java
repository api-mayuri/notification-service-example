package kz.company.notification.infrastructure.in.messaging;

import kz.company.notification.application.port.in.DeliveryOutcome;
import kz.company.notification.application.port.in.ProcessDeliveryUseCase;
import kz.company.notification.domain.delivery.DeliveryId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Воркер канала: читает топик доставок одного канала и одной полосы приоритета. Экземпляр создаётся
 * на каждый включённый канал и полосу (см. {@code DeliveryListenerRegistrar} в bootstrap), поэтому
 * у каждого канала свой топик, consumer group и число потребителей.
 *
 * <p>Итог обработки переводится в поведение брокера:
 * <ul>
 *     <li>RETRY_SCHEDULED — {@link RetryableDeliveryException}: запись уходит в retry-топик с задержкой;</li>
 *     <li>FAILED — {@link DeliveryFailedException}: запись сразу уходит в DLT (parking lot);</li>
 *     <li>остальное — запись подтверждается.</li>
 * </ul>
 */
public class ChannelDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(ChannelDeliveryListener.class);

    private final String listenerId;
    private final String topic;
    private final String groupId;
    private final String concurrency;
    private final ProcessDeliveryUseCase processDeliveryUseCase;
    private final JsonMapper jsonMapper;

    public ChannelDeliveryListener(String listenerId, String topic, String groupId, int concurrency,
                                   ProcessDeliveryUseCase processDeliveryUseCase, JsonMapper jsonMapper) {
        this.listenerId = listenerId;
        this.topic = topic;
        this.groupId = groupId;
        this.concurrency = Integer.toString(concurrency);
        this.processDeliveryUseCase = processDeliveryUseCase;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(id = "#{__listener.listenerId}", topics = "#{__listener.topic}",
            groupId = "#{__listener.groupId}", concurrency = "#{__listener.concurrency}")
    public void onMessage(ConsumerRecord<String, String> record) {
        DeliveryId deliveryId = deliveryId(jsonMapper, record);
        DeliveryOutcome outcome = processDeliveryUseCase.process(deliveryId);
        switch (outcome) {
            case DeliveryOutcome.RetryScheduled retry -> throw new RetryableDeliveryException(
                    "Delivery " + deliveryId + " will be retried: " + retry.reason());
            case DeliveryOutcome.Failed failed -> throw new DeliveryFailedException(
                    "Delivery " + deliveryId + " failed" + (failed.permanent() ? " permanently" : "") + ": " + failed.reason());
            case DeliveryOutcome.Sent sent -> log.debug("Delivery {} sent providerMessageId={} simulated={}",
                    deliveryId, sent.providerMessageId(), sent.simulated());
            case DeliveryOutcome.Deferred deferred -> log.info("Delivery {} deferred until {}", deliveryId, deferred.until());
            case DeliveryOutcome.Ignored ignored -> log.debug("Delivery {} ignored: {}", deliveryId, ignored.reason());
        }
    }

    static DeliveryId deliveryId(JsonMapper jsonMapper, ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            throw new InvalidMessageException("Empty delivery message");
        }
        try {
            JsonNode node = jsonMapper.readTree(record.value());
            JsonNode id = node.get("deliveryId");
            if (id == null || id.isNull()) {
                throw new InvalidMessageException("Delivery message has no deliveryId");
            }
            return DeliveryId.of(id.asString());
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidMessageException("Malformed delivery message", exception);
        }
    }

    public String getListenerId() {
        return listenerId;
    }

    public String getTopic() {
        return topic;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getConcurrency() {
        return concurrency;
    }
}
