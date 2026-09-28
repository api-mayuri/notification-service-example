package kz.company.notification.application.port.in;

/**
 * Принять уведомление: проверить, выбрать каналы, отрендерить, сохранить уведомление и
 * доставки и поставить готовые доставки в очередь — в одной транзакции.
 *
 * <p>Идемпотентность: повтор с тем же ключом от того же отправителя возвращает исходный
 * результат, с другим телом — ошибку {@code IDEMPOTENCY_KEY_REUSED}.
 */
public interface SendNotificationUseCase {

    SendNotificationResult send(SendNotificationCommand command);
}
