package kz.company.notification.bootstrap.config;

import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.infrastructure.in.messaging.DeliveryDeadLetterHandler;
import kz.company.notification.infrastructure.in.messaging.DeliveryFailedException;
import kz.company.notification.infrastructure.in.messaging.InvalidMessageException;
import kz.company.notification.infrastructure.in.messaging.RejectedMessageException;
import kz.company.notification.infrastructure.messaging.CloudEventHeaders;
import kz.company.notification.infrastructure.messaging.ExternalMessage;
import kz.company.notification.infrastructure.messaging.NotificationKafkaProperties;
import kz.company.notification.infrastructure.messaging.TopicNames;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Kafka: топики (объявляются бинами, а не вручную), обработка ошибок, неблокирующие повторы
 * доставок через retry-топики и externalization событий outbox.
 */
@Configuration(proxyBeanMethods = false)
public class KafkaConfiguration {

    /** Воркеры каналов: по бину на включённый канал и полосу приоритета. */
    @Bean
    static DeliveryListenerRegistrar deliveryListenerRegistrar() {
        return new DeliveryListenerRegistrar();
    }

    /**
     * Выделенный исполнитель для Kafka consumer threads. Виртуальные потоки устраняют ограничение
     * platform thread pool при запуске большого числа listener containers одновременно.
     */
    @Bean
    SimpleAsyncTaskExecutor kafkaListenerExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-listener-");
        executor.setVirtualThreads(true);
        return executor;
    }

    /**
     * Применяет {@link #kafkaListenerExecutor()} к автоконфигурированной фабрике по умолчанию
     * (используется {@code @KafkaListener} без явного containerFactory).
     */
    @Bean
    ContainerCustomizer<Object, Object, KafkaMessageListenerContainer<Object, Object>> kafkaContainerCustomizer(
            SimpleAsyncTaskExecutor kafkaListenerExecutor) {
        return container -> container.getContainerProperties().setListenerTaskExecutor(kafkaListenerExecutor);
    }

    /**
     * Основные топики и DLT. Retry-топики доставок создаёт {@link RetryTopicConfiguration}.
     * {@code min.insync.replicas} не больше фактора репликации (в dev — 1 брокер).
     */
    @Bean
    KafkaAdmin.NewTopics notificationTopics(NotificationKafkaProperties properties, TopicNames topicNames,
                                            ChannelSenderRegistry channelSenderRegistry) {
        List<NewTopic> topics = new ArrayList<>();
        topics.add(topic(properties, topicNames.sendRequests(), properties.retention()));
        topics.add(topic(properties, topicNames.deadLetter(topicNames.sendRequests()), properties.deadLetterRetention()));
        topics.add(topic(properties, topicNames.events(), properties.retention()));
        for (ChannelType channel : channelSenderRegistry.enabledChannels()) {
            for (String deliveryTopic : topicNames.deliveryTopics(channel)) {
                topics.add(topic(properties, deliveryTopic, properties.retention()));
                topics.add(topic(properties, topicNames.deadLetter(deliveryTopic), properties.deadLetterRetention()));
            }
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    private static NewTopic topic(NotificationKafkaProperties properties, String name, java.time.Duration retention) {
        int minInsync = Math.min(properties.minInsyncReplicas(), properties.replicationFactor());
        return TopicBuilder.name(name)
                .partitions(properties.partitions())
                .replicas(properties.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, Long.toString(retention.toMillis()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, Integer.toString(minInsync))
                .build();
    }

    /**
     * Контейнер входящих команд: подтверждение после каждой записи, повторы с backoff для временных
     * ошибок, невалидные и отклонённые сообщения — сразу в {@code notification.send.requests.v1-dlt}.
     */
    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> sendRequestContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory, KafkaTemplate<Object, Object> kafkaTemplate,
            NotificationKafkaProperties properties, SimpleAsyncTaskExecutor kafkaListenerExecutor) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(Math.min(3, properties.partitions()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setListenerTaskExecutor(kafkaListenerExecutor);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + properties.deadLetterSuffix(), record.partition()));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30_000);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidMessageException.class, RejectedMessageException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * Неблокирующие повторы доставок: временная ошибка — запись уходит в retry-топик с задержкой
     * по той же политике, что и в домене; окончательная ошибка — сразу в DLT. Порядок сообщений
     * одного получателя при повторах не сохраняется (docs/adr/0005-kafka-topology.md).
     */
    @Bean
    RetryTopicConfiguration deliveryRetryTopics(KafkaTemplate<Object, Object> kafkaTemplate,
                                                NotificationProperties notificationProperties,
                                                NotificationKafkaProperties kafkaProperties, TopicNames topicNames,
                                                ChannelSenderRegistry channelSenderRegistry) {
        NotificationProperties.Retry retry = notificationProperties.retry();
        List<String> deliveryTopics = channelSenderRegistry.enabledChannels().stream()
                .flatMap(channel -> topicNames.deliveryTopics(channel).stream())
                .toList();
        return RetryTopicConfigurationBuilder.newInstance()
                .exponentialBackoff(retry.initialDelay().toMillis(), retry.multiplier(), retry.maxDelay().toMillis())
                .maxAttempts(retry.maxAttempts())
                .includeTopics(deliveryTopics.isEmpty() ? List.of("__no_delivery_topics__") : deliveryTopics)
                .notRetryOn(List.of(DeliveryFailedException.class, InvalidMessageException.class))
                .retryTopicSuffix("-retry")
                .dltSuffix(kafkaProperties.deadLetterSuffix())
                .autoCreateTopicsWith(kafkaProperties.partitions(), kafkaProperties.replicationFactor())
                .dltHandlerMethod(DeliveryDeadLetterHandler.BEAN_NAME, DeliveryDeadLetterHandler.METHOD_NAME)
                .create(kafkaTemplate);
    }

    /**
     * Externalization outbox: в Kafka уходят только {@link ExternalMessage}; топик и ключ — из сообщения,
     * атрибуты CloudEvents — в заголовках. Доменные события наружу напрямую не публикуются.
     */
    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .select(event -> event instanceof ExternalMessage)
                .route(ExternalMessage.class, message -> RoutingTarget.forTarget(message.topic()).andKey(message.key()))
                .headers(ExternalMessage.class, CloudEventHeaders::of)
                .build();
    }
}
