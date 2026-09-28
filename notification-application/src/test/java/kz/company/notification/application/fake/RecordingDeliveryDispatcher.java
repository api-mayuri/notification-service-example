package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.DeliveryDispatcher;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;

import java.util.ArrayList;
import java.util.List;

public final class RecordingDeliveryDispatcher implements DeliveryDispatcher {

    private final List<DeliveryId> dispatched = new ArrayList<>();

    @Override
    public void dispatch(Delivery delivery) {
        dispatched.add(delivery.id());
    }

    public List<DeliveryId> dispatched() {
        return dispatched;
    }

    public void clear() {
        dispatched.clear();
    }
}
