package kz.company.notification.infrastructure.messaging;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Priority;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingSupportTest {

    private static NotificationKafkaProperties properties() {
        return new NotificationKafkaProperties("urn:test", List.of(), "notification-service",
                "notification.send.requests.v1", "notification.events.v1", "notification.delivery", "v1", "-dlt",
                12, (short) 3, 2, Duration.ofDays(7), Duration.ofDays(30));
    }

    @Test
    void topicNamesFollowConvention() {
        TopicNames topics = new TopicNames(properties());

        assertThat(topics.delivery(ChannelType.EMAIL, Priority.NORMAL)).isEqualTo("notification.delivery.email.v1");
        assertThat(topics.delivery(ChannelType.SMS, Priority.HIGH)).isEqualTo("notification.delivery.sms.high.v1");
        assertThat(topics.deadLetter(topics.sendRequests())).isEqualTo("notification.send.requests.v1-dlt");
        assertThat(topics.deliveryConsumerGroup(ChannelType.PUSH, TopicNames.Lane.HIGH))
                .isEqualTo("notification-service.delivery.push.high");
        assertThat(topics.deliveryTopics(ChannelType.WEBHOOK)).hasSize(2);
    }

    @Test
    void cloudEventHeadersAreWrittenAndRead() {
        ExternalMessage message = new ExternalMessage("topic", "key", "id-1", "urn:test", "type.v1", "subject",
                Instant.parse("2026-09-25T10:00:00Z"), "{\"a\":1}");

        Map<String, Object> headers = CloudEventHeaders.of(message);

        assertThat(headers).containsEntry("ce_id", "id-1").containsEntry("ce_specversion", "1.0")
                .containsEntry("ce_time", "2026-09-25T10:00:00Z").containsEntry("content-type", "application/json");

        RecordHeaders incoming = new RecordHeaders();
        incoming.add("ce_source", "\"urn:quoted\"".getBytes(StandardCharsets.UTF_8));
        incoming.add("ce_id", "plain".getBytes(StandardCharsets.UTF_8));
        assertThat(CloudEventHeaders.read(incoming, "ce_source")).contains("urn:quoted");
        assertThat(CloudEventHeaders.read(incoming, "ce_id")).contains("plain");
        assertThat(CloudEventHeaders.read(incoming, "ce_type")).isEmpty();
    }

    @Test
    void serializerWritesPayloadAndPassesThroughRawValues() {
        ExternalMessageSerializer serializer = new ExternalMessageSerializer();
        ExternalMessage message = new ExternalMessage("t", "k", "i", "s", "type", null, Instant.EPOCH, "{\"x\":true}");

        assertThat(new String(serializer.serialize("t", message), StandardCharsets.UTF_8)).isEqualTo("{\"x\":true}");
        assertThat(new String(serializer.serialize("t", "text"), StandardCharsets.UTF_8)).isEqualTo("text");
        assertThat(serializer.serialize("t", new byte[]{1, 2})).containsExactly((byte) 1, (byte) 2);
        assertThat(serializer.serialize("t", null)).isNull();
        assertThat(MessageTypes.event("delivery.sent")).isEqualTo("kz.company.notification.delivery.sent.v1");
    }
}
