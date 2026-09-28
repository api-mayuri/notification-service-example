package kz.company.notification.application.port.in;

/**
 * Поставить в очередь доставки, у которых подошло время (sendAt, конец тихих часов,
 * окончание лимита частоты). Безопасно для нескольких экземпляров.
 */
public interface DispatchDueDeliveriesUseCase {

    /** @return число поставленных в очередь доставок */
    int dispatchDue();
}
