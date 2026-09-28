package kz.company.notification.bootstrap.config;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Часть настроек канала ({@code notification.channels.<канал>.*}), нужная для слушателей Kafka.
 *
 * @param concurrency             потребителей обычной полосы (не больше числа партиций)
 * @param highPriorityConcurrency потребителей полосы HIGH
 */
public record ChannelListenerSettings(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("3") int concurrency,
        @DefaultValue("6") int highPriorityConcurrency) {
}
