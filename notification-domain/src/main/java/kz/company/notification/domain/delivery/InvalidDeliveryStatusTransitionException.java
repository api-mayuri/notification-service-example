package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

/** Недопустимый переход в машине состояний доставки. */
public class InvalidDeliveryStatusTransitionException extends DomainException {

    public InvalidDeliveryStatusTransitionException(DeliveryId id, DeliveryStatus from, DeliveryStatus to) {
        super("INVALID_DELIVERY_STATUS_TRANSITION", DomainErrorKind.CONFLICT,
                "Delivery " + id + " cannot move from " + from + " to " + to);
    }

    public InvalidDeliveryStatusTransitionException(DeliveryId id, DeliveryStatus current, String operation) {
        super("INVALID_DELIVERY_STATUS_TRANSITION", DomainErrorKind.CONFLICT,
                "Delivery " + id + " cannot " + operation + " in status " + current);
    }
}
