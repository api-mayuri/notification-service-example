package kz.company.notification.domain.notification;

/** Итоговый статус уведомления. Вычисляется из статусов доставок. */
public enum NotificationStatus {
    /** Принято, ни одна доставка ещё не начиналась. */
    ACCEPTED,
    /** Хотя бы одна доставка в работе. */
    IN_PROGRESS,
    /** Все доставки завершены, неудачных нет. */
    COMPLETED,
    /** Часть доставок успешна, часть завершилась ошибкой. */
    PARTIALLY_FAILED,
    /** Ни одна доставка не прошла. */
    FAILED,
    /** Отменено до отправки. */
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == PARTIALLY_FAILED || this == FAILED || this == CANCELLED;
    }
}
