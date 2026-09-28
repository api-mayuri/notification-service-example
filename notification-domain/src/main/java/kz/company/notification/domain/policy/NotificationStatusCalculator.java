package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.notification.NotificationStatus;

import java.util.Collection;

/**
 * Статус уведомления из статусов его доставок.
 *
 * <ul>
 *     <li>есть незавершённые доставки: ACCEPTED, если ни одна не начиналась, иначе IN_PROGRESS;</li>
 *     <li>все завершены и все отменены: CANCELLED;</li>
 *     <li>нет неудачных: COMPLETED (пропущенные и отменённые не считаются неудачей);</li>
 *     <li>есть и успешные, и неудачные: PARTIALLY_FAILED;</li>
 *     <li>только неудачные: FAILED.</li>
 * </ul>
 */
public final class NotificationStatusCalculator {

    private NotificationStatusCalculator() {
    }

    public static NotificationStatus calculate(Collection<DeliveryStatus> statuses) {
        if (statuses.isEmpty()) {
            return NotificationStatus.ACCEPTED;
        }
        boolean anyUnfinished = statuses.stream().anyMatch(status -> !status.isTerminal());
        if (anyUnfinished) {
            boolean nothingStarted = statuses.stream()
                    .allMatch(status -> status == DeliveryStatus.PENDING || status == DeliveryStatus.SKIPPED
                            || status == DeliveryStatus.CANCELLED);
            return nothingStarted ? NotificationStatus.ACCEPTED : NotificationStatus.IN_PROGRESS;
        }
        if (statuses.stream().allMatch(status -> status == DeliveryStatus.CANCELLED)) {
            return NotificationStatus.CANCELLED;
        }
        long failed = statuses.stream().filter(status -> status == DeliveryStatus.FAILED).count();
        long succeeded = statuses.stream().filter(DeliveryStatus::isSuccessful).count();
        if (failed == 0) {
            return NotificationStatus.COMPLETED;
        }
        return succeeded > 0 ? NotificationStatus.PARTIALLY_FAILED : NotificationStatus.FAILED;
    }
}
