package kz.company.notification.infrastructure.out.recipient;

import kz.company.notification.application.port.out.RecipientResolver;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Реализация по умолчанию, когда сервис пользователей не подключён: контакты передаются
 * только в запросе, по идентификатору ничего не находится.
 */
@Component
@ConditionalOnExpression("'${notification.recipient-directory.base-url:}'.isEmpty()")
public class RequestOnlyRecipientResolver implements RecipientResolver {

    @Override
    public RecipientContacts resolve(RecipientId recipientId) {
        return RecipientContacts.NONE;
    }
}
