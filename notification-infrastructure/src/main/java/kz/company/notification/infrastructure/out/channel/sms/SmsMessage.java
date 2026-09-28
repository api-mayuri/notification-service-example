package kz.company.notification.infrastructure.out.channel.sms;

import kz.company.notification.domain.delivery.PhoneNumber;

import java.util.UUID;

/** Сообщение канала SMS для провайдера. {@code deliveryId} — ключ идемпотентности у провайдера. */
public record SmsMessage(UUID deliveryId, PhoneNumber to, String text) {
}
