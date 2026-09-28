package kz.company.notification.infrastructure.out.channel.webhook;

import kz.company.notification.domain.delivery.WebhookUrl;

import java.util.UUID;

/** Вызов вебхука: адрес и содержимое уведомления. */
public record WebhookMessage(UUID deliveryId, UUID notificationId, String recipientId, WebhookUrl url,
                             String subject, String text, String html) {
}
