package kz.company.notification.infrastructure.out.channel.push;

import kz.company.notification.domain.delivery.DeviceToken;

import java.util.UUID;

/** Сообщение канала PUSH для провайдера. {@code deliveryId} — ключ идемпотентности у провайдера. */
public record PushMessage(UUID deliveryId, DeviceToken token, String title, String body) {
}
