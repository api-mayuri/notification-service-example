package kz.company.notification.infrastructure.in.messaging;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import kz.company.notification.application.port.in.SendNotificationCommand;
import kz.company.notification.application.port.in.SendNotificationResult;
import kz.company.notification.application.port.in.SendNotificationUseCase;
import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.infrastructure.in.common.NotificationCommandMapper;
import kz.company.notification.infrastructure.in.common.NotificationRequestPayload;
import kz.company.notification.infrastructure.messaging.CloudEventHeaders;
import kz.company.notification.infrastructure.messaging.MessageTypes;
import kz.company.notification.infrastructure.messaging.NotificationKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Входящий адаптер Kafka: команды на отправку из {@code notification.send.requests.v1}
 * (CloudEvents, binary mode). Переводит сообщение в ту же {@link SendNotificationCommand},
 * что и REST, и вызывает тот же {@link SendNotificationUseCase}.
 *
 * <ul>
 *     <li>отправитель — {@code ce_source};</li>
 *     <li>ключ идемпотентности — {@code idempotencyKey} из данных, иначе {@code ce_id};</li>
 *     <li>готовое содержимое без шаблона — только для источников из {@code notification.kafka.trusted-sources}.</li>
 * </ul>
 * Невалидное или отклонённое сообщение уходит в {@code notification.send.requests.v1-dlt} без повторов;
 * временные ошибки повторяются обработчиком ошибок контейнера.
 */
@Component
public class SendRequestListener {

    private static final Logger log = LoggerFactory.getLogger(SendRequestListener.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final NotificationCommandMapper commandMapper;
    private final JsonMapper jsonMapper;
    private final Validator validator;
    private final Set<String> trustedSources;

    public SendRequestListener(SendNotificationUseCase sendNotificationUseCase, NotificationCommandMapper commandMapper,
                               JsonMapper jsonMapper, Validator validator, NotificationKafkaProperties properties) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.commandMapper = commandMapper;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
        this.trustedSources = Set.copyOf(properties.trustedSources());
    }

    @KafkaListener(id = "send-requests",
            topics = "${notification.kafka.send-requests-topic:notification.send.requests.v1}",
            groupId = "${notification.kafka.consumer-group-prefix:notification-service}.send-requests",
            containerFactory = "sendRequestContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        String type = header(record, CloudEventHeaders.TYPE);
        if (!MessageTypes.SEND_REQUEST.equals(type)) {
            throw new InvalidMessageException("Unsupported ce_type '" + type + "', expected " + MessageTypes.SEND_REQUEST);
        }
        String specVersion = header(record, CloudEventHeaders.SPEC_VERSION);
        if (!CloudEventHeaders.SPEC_VERSION_VALUE.equals(specVersion)) {
            throw new InvalidMessageException("Unsupported ce_specversion '" + specVersion + "'");
        }
        String source = header(record, CloudEventHeaders.SOURCE);
        String eventId = header(record, CloudEventHeaders.ID);
        NotificationRequestPayload payload = parse(record);

        try {
            ClientId clientId = new ClientId(source);
            IdempotencyKey key = new IdempotencyKey(payload.idempotencyKey() != null ? payload.idempotencyKey() : eventId);
            SendNotificationCommand command = commandMapper.toCommand(payload, clientId, key, trustedSources.contains(source));
            SendNotificationResult result = sendNotificationUseCase.send(command);
            log.info("Kafka send request accepted source={} ceId={} notificationId={} outcome={}", source, eventId,
                    result.notificationId(), result.outcome());
        } catch (DomainException exception) {
            if (exception.kind() == DomainErrorKind.CONFLICT) {
                throw exception;
            }
            throw new RejectedMessageException(exception.errorCode(), exception.getMessage(), exception);
        }
    }

    private NotificationRequestPayload parse(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw new InvalidMessageException("Empty message value");
        }
        NotificationRequestPayload payload;
        try {
            payload = jsonMapper.readValue(record.value(), NotificationRequestPayload.class);
        } catch (JacksonException exception) {
            throw new InvalidMessageException("Message value is not a valid notification request: "
                    + exception.getOriginalMessage(), exception);
        }
        Set<ConstraintViolation<NotificationRequestPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new InvalidMessageException("Invalid notification request: " + violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return payload;
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return CloudEventHeaders.read(record.headers(), name)
                .orElseThrow(() -> new InvalidMessageException("Missing CloudEvents header " + name));
    }
}
