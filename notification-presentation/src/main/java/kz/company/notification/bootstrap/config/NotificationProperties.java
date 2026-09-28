package kz.company.notification.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Общие настройки сервиса ({@code notification.*}). Настройки каналов, Kafka и планировщика —
 * в своих классах свойств рядом с адаптерами.
 *
 * @param defaultLocale локаль шаблонов по умолчанию (последнее звено fallback)
 */
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @DefaultValue("ru") String defaultLocale,
        @DefaultValue Retry retry,
        @DefaultValue Delivery delivery) {

    /**
     * Повторы доставки: одна политика для домена (подсчёт попыток, jitter) и для retry-топиков Kafka.
     *
     * @param maxAttempts общее число попыток, включая первую
     * @param jitter      доля разброса задержки (0..1)
     */
    public record Retry(
            @DefaultValue("5") int maxAttempts,
            @DefaultValue("30s") Duration initialDelay,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("30m") Duration maxDelay,
            @DefaultValue("0.2") double jitter) {
    }

    /**
     * @param rateLimits лимиты частоты по каналам: ключ — код канала в нижнем регистре
     */
    public record Delivery(
            @DefaultValue("5m") Duration staleSendingThreshold,
            @DefaultValue("10m") Duration overdueRetryGrace,
            @DefaultValue("200") int batchSize,
            @DefaultValue("24h") Duration idempotencyKeyRetention,
            @DefaultValue Map<String, RateLimitProperties> rateLimits) {
    }

    /**
     * @param categories категории, к которым применяется лимит (по умолчанию MARKETING)
     */
    public record RateLimitProperties(int max, Duration window, Set<String> categories) {
    }
}
