package kz.company.notification.infrastructure.out.channel.whatsapp;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки канала WHATSAPP ({@code notification.channels.whatsapp.*}).
 *
 * @param provider       реализация {@link WhatsAppProvider}: {@code logging} (заглушка) или имя реального провайдера
 * @param maxConcurrency сколько отправок по каналу выполняется одновременно
 * @param stub           поведение заглушки (случайные сбои для проверки повторов)
 */
@ConfigurationProperties(prefix = "notification.channels.whatsapp")
public record WhatsAppChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("logging") String provider,
        @DefaultValue("10") int maxConcurrency,
        @DefaultValue StubBehaviour stub) {
}
