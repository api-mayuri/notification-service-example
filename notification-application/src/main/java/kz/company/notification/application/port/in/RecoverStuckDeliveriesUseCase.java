package kz.company.notification.application.port.in;

/**
 * Найти «зависшие» доставки (SENDING дольше порога, просроченные повторы) и вернуть их в очередь.
 */
public interface RecoverStuckDeliveriesUseCase {

    /** @return число восстановленных доставок */
    int recover();
}
