package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер webhook. Реализация выбирается настройкой {@code notification.channels.webhook.provider}.
 *
 * <p>Контракт: 2xx — {@code Success}; сеть, таймаут, 408, 425, 429, 5xx — {@code RetryableFailure};
 * остальные 4xx — {@code PermanentFailure}. Исключения наружу не бросает.
 */
public interface WebhookProvider {

    SendResult send(WebhookMessage message);
}
