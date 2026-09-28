package kz.company.notification.infrastructure.out.channel.telegram;

import kz.company.notification.domain.delivery.TelegramChatId;

import java.util.UUID;

/** Сообщение канала TELEGRAM для провайдера. {@code deliveryId} — ключ идемпотентности у провайдера. */
public record TelegramMessage(UUID deliveryId, TelegramChatId chatId, String text) {
}
