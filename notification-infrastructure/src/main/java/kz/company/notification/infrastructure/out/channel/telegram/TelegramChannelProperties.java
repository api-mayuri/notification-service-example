package kz.company.notification.infrastructure.out.channel.telegram;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки канала TELEGRAM ({@code notification.channels.telegram.*}).
 *
 * @param provider       реализация {@link TelegramProvider}: {@code logging} (заглушка) или имя реального провайдера
 * @param maxConcurrency сколько отправок по каналу выполняется одновременно
 * @param stub           поведение заглушки (случайные сбои для проверки повторов)
 */
@ConfigurationProperties(prefix = "notification.channels.telegram")
public record TelegramChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("logging") String provider,
        @DefaultValue("20") int maxConcurrency,
        @DefaultValue StubBehaviour stub) {
}
