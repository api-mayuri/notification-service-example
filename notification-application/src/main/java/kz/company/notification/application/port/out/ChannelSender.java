package kz.company.notification.application.port.out;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.domain.notification.ContactType;

/**
 * Отправка по одному каналу. Точка расширения: новый канал — это новая реализация этого
 * интерфейса (плюс настройки), без изменения домена и существующих адаптеров.
 *
 * <p>Контракт:
 * <ul>
 *     <li>{@link #send} не бросает исключений из-за ответа провайдера: временная ошибка
 *     возвращается как {@link SendResult.RetryableFailure}, постоянная — как
 *     {@link SendResult.PermanentFailure}. Непредвиденное исключение вызывающая сторона
 *     трактует как временную ошибку.</li>
 *     <li>Метод может вызываться повторно для той же доставки (at-least-once): если провайдер
 *     поддерживает ключ идемпотентности, передавайте в него {@code delivery.id()}.</li>
 * </ul>
 *
 * <p>Пошаговый пример — в {@code docs/how-to-add-channel.md}.
 */
public interface ChannelSender {

    /** Канал, который обслуживает отправитель. */
    ChannelType channel();

    /** Какой контакт получателя нужен каналу. */
    ContactType contactType();

    /**
     * Проверить и разобрать адрес из контакта.
     *
     * @throws kz.company.notification.domain.common.DomainValidationException если формат неверный
     */
    ChannelAddress parseAddress(String rawAddress);

    /** Отправить доставку провайдеру канала. */
    SendResult send(Delivery delivery);
}
