package kz.company.notification.infrastructure.out.channel.telegram;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер канала TELEGRAM: Telegram Bot API (sendMessage). Реализация выбирается настройкой
 * {@code notification.channels.telegram.provider} через {@code @ConditionalOnProperty}.
 *
 * <p>Контракт:
 * <ul>
 *     <li>принимает {@link TelegramMessage} с уже проверенным адресом;</li>
 *     <li>возвращает {@code SendResult.Success} с идентификатором сообщения у провайдера;</li>
 *     <li>Временные: сеть, 429 Too Many Requests (с retry_after), 5xx Bot API. Постоянные: 400 chat not found, 403 bot was blocked by the user.</li>
 *     <li>временная ошибка — {@code RetryableFailure}, постоянная — {@code PermanentFailure};
 *     исключения наружу не бросает;</li>
 *     <li>может вызываться повторно для той же доставки: передавайте {@code deliveryId} провайдеру
 *     как ключ идемпотентности, если он это поддерживает.</li>
 * </ul>
 * Как заменить заглушку реальным провайдером — {@code docs/how-to-add-channel.md}.
 */
public interface TelegramProvider {

    SendResult send(TelegramMessage message);
}
