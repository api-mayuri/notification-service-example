package kz.company.notification.domain.notification;

/**
 * Категория уведомления определяет, какие правила предпочтений к нему применимы.
 */
public enum Category {
    /** Ответ на действие пользователя (сброс пароля, заказ). Не отключается отпиской, игнорирует тихие часы. */
    TRANSACTIONAL,
    /** Рекламные и информационные рассылки. Подчиняются отпискам и тихим часам. */
    MARKETING,
    /** Служебные уведомления платформы (безопасность, изменение условий). Ведут себя как TRANSACTIONAL. */
    SYSTEM;

    /** Можно ли отключить категорию отпиской. */
    public boolean isOptional() {
        return this == MARKETING;
    }

    /** Откладывается ли отправка на конец тихих часов получателя. */
    public boolean respectsQuietHours() {
        return this == MARKETING;
    }
}
