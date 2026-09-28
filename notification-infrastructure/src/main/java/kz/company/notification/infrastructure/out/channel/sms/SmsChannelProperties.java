package kz.company.notification.infrastructure.out.channel.sms;

import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки канала SMS ({@code notification.channels.sms.*}).
 *
 * @param provider       реализация {@link SmsProvider}: {@code logging} (заглушка) или имя реального провайдера
 * @param maxConcurrency сколько отправок по каналу выполняется одновременно
 * @param stub           поведение заглушки (случайные сбои для проверки повторов)
 */
@ConfigurationProperties(prefix = "notification.channels.sms")
public record SmsChannelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("logging") String provider,
        @DefaultValue("10") int maxConcurrency,
        @DefaultValue StubBehaviour stub) {
}
