package kz.company.notification.domain.delivery;

/** Причина, по которой доставка по каналу не выполнялась. */
public enum SkipReason {
    /** Получатель отписался от канала. */
    UNSUBSCRIBED_CHANNEL,
    /** Получатель отписался от категории. */
    UNSUBSCRIBED_CATEGORY,
    /** Нет контакта получателя для этого канала. */
    NO_CONTACT
}
