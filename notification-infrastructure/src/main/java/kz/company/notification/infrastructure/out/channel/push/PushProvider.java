package kz.company.notification.infrastructure.out.channel.push;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер канала PUSH: Firebase Cloud Messaging (HTTP v1) или APNs. Реализация выбирается настройкой
 * {@code notification.channels.push.provider} через {@code @ConditionalOnProperty}.
 *
 * <p>Контракт:
 * <ul>
 *     <li>принимает {@link PushMessage} с уже проверенным адресом;</li>
 *     <li>возвращает {@code SendResult.Success} с идентификатором сообщения у провайдера;</li>
 *     <li>Временные: сеть, 429 и 5xx (UNAVAILABLE, INTERNAL). Постоянные: UNREGISTERED, INVALID_ARGUMENT (токен устарел или неверен).</li>
 *     <li>временная ошибка — {@code RetryableFailure}, постоянная — {@code PermanentFailure};
 *     исключения наружу не бросает;</li>
 *     <li>может вызываться повторно для той же доставки: передавайте {@code deliveryId} провайдеру
 *     как ключ идемпотентности, если он это поддерживает.</li>
 * </ul>
 * Как заменить заглушку реальным провайдером — {@code docs/how-to-add-channel.md}.
 */
public interface PushProvider {

    SendResult send(PushMessage message);
}
