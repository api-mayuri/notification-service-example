package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.domain.delivery.SendResult;

/**
 * Провайдер email. Реализация выбирается настройкой {@code notification.channels.email.provider}.
 *
 * <p>Контракт:
 * <ul>
 *     <li>принимает готовое письмо ({@link EmailMessage}) с проверенным адресом;</li>
 *     <li>возвращает {@code Success} с Message-ID, если сервер принял письмо;</li>
 *     <li>временные ошибки (сеть, таймаут, SMTP 4xx, ошибка аутентификации) — {@code RetryableFailure};</li>
 *     <li>постоянные ошибки (SMTP 5xx: нет ящика, отказ в релее; некорректное письмо) — {@code PermanentFailure};</li>
 *     <li>исключения наружу не бросает.</li>
 * </ul>
 * Будущая реализация для Microsoft 365 / Google Workspace с OAuth2 (XOAUTH2) — отдельный класс
 * с {@code provider=smtp-oauth2}, без изменения {@link EmailChannelSender}.
 */
public interface EmailProvider {

    SendResult send(EmailMessage message);
}
