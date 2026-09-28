package kz.company.notification.infrastructure.in.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Интервалы задач планировщика ({@code notification.scheduler.*}).
 *
 * @param outboxResubmitAfter через сколько незавершённая публикация outbox отправляется повторно
 */
@ConfigurationProperties(prefix = "notification.scheduler")
public record SchedulingProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("5s") Duration dispatchInterval,
        @DefaultValue("1m") Duration recoveryInterval,
        @DefaultValue("1h") Duration idempotencyCleanupInterval,
        @DefaultValue("1m") Duration outboxResubmitInterval,
        @DefaultValue("2m") Duration outboxResubmitAfter) {
}
