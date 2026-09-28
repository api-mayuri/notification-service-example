package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;

public class DeliveryNotFoundException extends DomainException {

    public DeliveryNotFoundException(DeliveryId id) {
        super("DELIVERY_NOT_FOUND", DomainErrorKind.NOT_FOUND, "Delivery " + id + " not found");
    }
}
