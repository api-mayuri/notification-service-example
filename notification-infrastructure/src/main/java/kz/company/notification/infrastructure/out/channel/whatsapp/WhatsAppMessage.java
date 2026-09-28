package kz.company.notification.infrastructure.out.channel.whatsapp;

import kz.company.notification.domain.delivery.PhoneNumber;

import java.util.UUID;

/** Сообщение канала WHATSAPP для провайдера. {@code deliveryId} — ключ идемпотентности у провайдера. */
public record WhatsAppMessage(UUID deliveryId, PhoneNumber to, String text) {
}
