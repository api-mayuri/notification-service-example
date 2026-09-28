package kz.company.notification.application.port.out;

import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;

/**
 * Получение контактов получателя по его идентификатору (например, из сервиса пользователей).
 * Вызывается, только если нужных контактов нет в запросе.
 *
 * <p>Недоступность внешнего сервиса — временная ошибка: реализация бросает исключение,
 * и запрос отклоняется целиком (клиент повторит с тем же ключом идемпотентности).
 */
public interface RecipientResolver {

    /** @return известные контакты; пустой набор, если получатель не найден */
    RecipientContacts resolve(RecipientId recipientId);
}
