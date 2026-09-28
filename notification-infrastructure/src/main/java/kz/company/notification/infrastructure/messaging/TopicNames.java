package kz.company.notification.infrastructure.messaging;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Priority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Имена топиков:
 * <ul>
 *     <li>{@code notification.send.requests.v1} — входящие команды, ключ recipientId;</li>
 *     <li>{@code notification.delivery.<канал>.v1} и {@code notification.delivery.<канал>.high.v1} —
 *     доставки по каналу обычного и высокого приоритета, ключ recipientId;</li>
 *     <li>{@code notification.events.v1} — события статусов, ключ notificationId;</li>
 *     <li>{@code <топик>-dlt} — dead letter topic (parking lot).</li>
 * </ul>
 */
@Component
public final class TopicNames {

    private final NotificationKafkaProperties properties;

    public TopicNames(NotificationKafkaProperties properties) {
        this.properties = properties;
    }

    public String sendRequests() {
        return properties.sendRequestsTopic();
    }

    public String events() {
        return properties.eventsTopic();
    }

    public String deliveryLane(ChannelType channel, Lane lane) {
        String laneSegment = lane == Lane.HIGH ? ".high" : "";
        return properties.deliveryTopicPrefix() + "." + channel.lowerCaseCode() + laneSegment + "."
                + properties.deliveryTopicVersion();
    }

    public String delivery(ChannelType channel, Priority priority) {
        return deliveryLane(channel, Lane.of(priority));
    }

    public List<String> deliveryTopics(ChannelType channel) {
        return List.of(deliveryLane(channel, Lane.NORMAL), deliveryLane(channel, Lane.HIGH));
    }

    public String deadLetter(String topic) {
        return topic + properties.deadLetterSuffix();
    }

    public String deliveryConsumerGroup(ChannelType channel, Lane lane) {
        return properties.consumerGroupPrefix() + ".delivery." + channel.lowerCaseCode()
                + (lane == Lane.HIGH ? ".high" : "");
    }

    /** Полоса обработки: HIGH — отдельные топики и больше потребителей. */
    public enum Lane {
        NORMAL,
        HIGH;

        public static Lane of(Priority priority) {
            return priority == Priority.HIGH ? HIGH : NORMAL;
        }
    }
}
