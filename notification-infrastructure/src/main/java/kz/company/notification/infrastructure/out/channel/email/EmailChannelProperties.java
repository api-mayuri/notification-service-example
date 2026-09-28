package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки канала email ({@code notification.channels.email.*}). Подключение к SMTP —
 * стандартные свойства {@code spring.mail.*}.
 *
 * @param from                   адрес отправителя
 * @param fromName               отображаемое имя отправителя
 * @param replyTo                адрес для ответа (необязательно)
 * @param messageIdDomain        домен в заголовке Message-ID: {@code <deliveryId@domain>}
 * @param unsubscribeUrlTemplate ссылка отписки для заголовка List-Unsubscribe (marketing);
 *                               {@code {recipientId}} и {@code {deliveryId}} подставляются
 */
@ConfigurationProperties(prefix = "notification.channels.email")
public record EmailChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("smtp") String provider,
        @DefaultValue("20") int maxConcurrency,
        @DefaultValue("no-reply@example.com") String from,
        @DefaultValue("Notifications") String fromName,
        String replyTo,
        @DefaultValue("notification.local") String messageIdDomain,
        String unsubscribeUrlTemplate,
        @DefaultValue StubBehaviour stub) {
}
