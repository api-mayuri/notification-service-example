package kz.company.notification.infrastructure.out.recipient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Сервис пользователей ({@code notification.recipient-directory.*}). Если {@code base-url} не задан,
 * контакты берутся только из запроса.
 *
 * @param baseUrl     базовый адрес, например {@code http://users-service:8080}
 * @param bearerToken токен для вызова (из переменной окружения)
 */
@ConfigurationProperties(prefix = "notification.recipient-directory")
public record RecipientDirectoryProperties(
        String baseUrl,
        String bearerToken,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {
}
