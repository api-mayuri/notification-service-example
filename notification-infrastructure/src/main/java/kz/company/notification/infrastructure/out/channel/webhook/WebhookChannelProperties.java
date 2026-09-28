package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Настройки канала webhook ({@code notification.channels.webhook.*}).
 *
 * @param signingSecret      секрет HMAC-SHA256 для подписи тела; только из переменной окружения
 * @param requireHttps       отклонять адреса http:// (включено в prod)
 * @param networkRetries     быстрые повторы при сетевой ошибке внутри одной попытки
 * @param networkRetryDelay  задержка перед быстрым повтором, мс
 */
@ConfigurationProperties(prefix = "notification.channels.webhook")
public record WebhookChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("http") String provider,
        @DefaultValue("20") int maxConcurrency,
        String signingSecret,
        @DefaultValue("false") boolean requireHttps,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        @DefaultValue("2") int networkRetries,
        @DefaultValue("200") long networkRetryDelay,
        @DefaultValue StubBehaviour stub) {
}
