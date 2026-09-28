package kz.company.notification.infrastructure.out.channel.push;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки канала PUSH ({@code notification.channels.push.*}).
 *
 * @param provider       реализация {@link PushProvider}: {@code logging} (заглушка) или имя реального провайдера
 * @param maxConcurrency сколько отправок по каналу выполняется одновременно
 * @param stub           поведение заглушки (случайные сбои для проверки повторов)
 */
@ConfigurationProperties(prefix = "notification.channels.push")
public record PushChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("logging") String provider,
        @DefaultValue("50") int maxConcurrency,
        @DefaultValue StubBehaviour stub) {
}
