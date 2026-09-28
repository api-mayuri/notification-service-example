package kz.company.notification.infrastructure.out.channel.sms;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер канала SMS: SMS-шлюз (например, Twilio, SMSC.kz, Mobizon). Реализация выбирается настройкой
 * {@code notification.channels.sms.provider} через {@code @ConditionalOnProperty}.
 *
 * <p>Контракт:
 * <ul>
 *     <li>принимает {@link SmsMessage} с уже проверенным адресом;</li>
 *     <li>возвращает {@code SendResult.Success} с идентификатором сообщения у провайдера;</li>
 *     <li>Временные: таймаут, 5xx и 429 шлюза. Постоянные: неверный или заблокированный номер, отказ оператора.</li>
 *     <li>временная ошибка — {@code RetryableFailure}, постоянная — {@code PermanentFailure};
 *     исключения наружу не бросает;</li>
 *     <li>может вызываться повторно для той же доставки: передавайте {@code deliveryId} провайдеру
 *     как ключ идемпотентности, если он это поддерживает.</li>
 * </ul>
 * Как заменить заглушку реальным провайдером — {@code docs/how-to-add-channel.md}.
 */
public interface SmsProvider {

    SendResult send(SmsMessage message);
}
