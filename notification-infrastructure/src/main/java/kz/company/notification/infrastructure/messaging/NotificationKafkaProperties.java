package kz.company.notification.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Топики и параметры Kafka сервиса ({@code notification.kafka.*}).
 *
 * @param source              значение {@code ce_source} исходящих событий
 * @param trustedSources      {@code ce_source} отправителей, которым разрешено готовое содержимое без шаблона
 * @param consumerGroupPrefix префикс consumer group; группа канала — {@code <prefix>.delivery.<канал>}
 * @param partitions          число партиций основных топиков
 * @param replicationFactor   фактор репликации (prod — 3, dev — 1)
 * @param minInsyncReplicas   {@code min.insync.replicas} (prod — 2)
 * @param retention           хранение основных топиков
 * @param deadLetterRetention хранение DLT (parking lot)
 */
@ConfigurationProperties(prefix = "notification.kafka")
public record NotificationKafkaProperties(
        @DefaultValue("urn:kz.company:notification-service") String source,
        @DefaultValue List<String> trustedSources,
        @DefaultValue("notification-service") String consumerGroupPrefix,
        @DefaultValue("notification.send.requests.v1") String sendRequestsTopic,
        @DefaultValue("notification.events.v1") String eventsTopic,
        @DefaultValue("notification.delivery") String deliveryTopicPrefix,
        @DefaultValue("v1") String deliveryTopicVersion,
        @DefaultValue("-dlt") String deadLetterSuffix,
        @DefaultValue("12") int partitions,
        @DefaultValue("3") short replicationFactor,
        @DefaultValue("2") int minInsyncReplicas,
        @DefaultValue("7d") Duration retention,
        @DefaultValue("30d") Duration deadLetterRetention) {
}
