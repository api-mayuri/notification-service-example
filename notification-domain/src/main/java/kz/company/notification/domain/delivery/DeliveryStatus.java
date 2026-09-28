package kz.company.notification.domain.delivery;

import java.util.EnumSet;
import java.util.Set;

/**
 * Статусы доставки и допустимые переходы между ними.
 *
 * <pre>
 * PENDING → SENDING → SENT → DELIVERED (если провайдер подтверждает)
 *                   ↘ RETRY_SCHEDULED → SENDING
 *                   ↘ FAILED (постоянная ошибка или исчерпаны попытки)
 * PENDING → SKIPPED (отписка, нет контакта) | CANCELLED
 * RETRY_SCHEDULED → CANCELLED
 * PENDING | RETRY_SCHEDULED → FAILED (разбор в parking lot: сообщение попало в DLT)
 * </pre>
 */
public enum DeliveryStatus {
    PENDING,
    SENDING,
    RETRY_SCHEDULED,
    SENT,
    DELIVERED,
    FAILED,
    SKIPPED,
    CANCELLED;

    public Set<DeliveryStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> EnumSet.of(SENDING, SKIPPED, CANCELLED, FAILED);
            case SENDING -> EnumSet.of(SENT, RETRY_SCHEDULED, FAILED);
            case RETRY_SCHEDULED -> EnumSet.of(SENDING, CANCELLED, FAILED);
            case SENT -> EnumSet.of(DELIVERED);
            case DELIVERED, FAILED, SKIPPED, CANCELLED -> EnumSet.noneOf(DeliveryStatus.class);
        };
    }

    public boolean canTransitionTo(DeliveryStatus target) {
        return allowedTargets().contains(target);
    }

    public boolean isTerminal() {
        return allowedTargets().isEmpty() || this == SENT;
    }

    /** Ожидает отправки (ещё не отправлялась или ждёт повтора). */
    public boolean isAwaitingSend() {
        return this == PENDING || this == RETRY_SCHEDULED;
    }

    public boolean isSuccessful() {
        return this == SENT || this == DELIVERED;
    }
}
