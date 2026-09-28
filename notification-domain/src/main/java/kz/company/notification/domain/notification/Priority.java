package kz.company.notification.domain.notification;

/** Приоритет уведомления. HIGH обрабатывается в отдельной «полосе» брокера. */
public enum Priority {
    LOW,
    NORMAL,
    HIGH
}
