package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.domain.delivery.EmailAddress;

import java.util.UUID;

/**
 * Письмо для провайдера.
 *
 * @param htmlBody       HTML-версия (может отсутствовать)
 * @param textBody       текстовая версия (есть всегда)
 * @param unsubscribeUrl ссылка для List-Unsubscribe; есть только у marketing-писем
 */
public record EmailMessage(UUID deliveryId, String recipientId, EmailAddress to, String subject, String textBody,
                           String htmlBody, String unsubscribeUrl) {
}
