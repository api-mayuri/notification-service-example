package kz.company.notification.infrastructure.out.channel.whatsapp;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер канала WHATSAPP: WhatsApp Business Cloud API. Реализация выбирается настройкой
 * {@code notification.channels.whatsapp.provider} через {@code @ConditionalOnProperty}.
 *
 * <p>Контракт:
 * <ul>
 *     <li>принимает {@link WhatsAppMessage} с уже проверенным адресом;</li>
 *     <li>возвращает {@code SendResult.Success} с идентификатором сообщения у провайдера;</li>
 *     <li>Временные: сеть, 429 и 5xx Cloud API. Постоянные: номер не зарегистрирован в WhatsApp, истекло 24-часовое окно без шаблона.</li>
 *     <li>временная ошибка — {@code RetryableFailure}, постоянная — {@code PermanentFailure};
 *     исключения наружу не бросает;</li>
 *     <li>может вызываться повторно для той же доставки: передавайте {@code deliveryId} провайдеру
 *     как ключ идемпотентности, если он это поддерживает.</li>
 * </ul>
 * Как заменить заглушку реальным провайдером — {@code docs/how-to-add-channel.md}.
 */
public interface WhatsAppProvider {

    SendResult send(WhatsAppMessage message);
}
