package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.notification.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kz.company.notification.domain.delivery.DeliveryStatus.CANCELLED;
import static kz.company.notification.domain.delivery.DeliveryStatus.DELIVERED;
import static kz.company.notification.domain.delivery.DeliveryStatus.FAILED;
import static kz.company.notification.domain.delivery.DeliveryStatus.PENDING;
import static kz.company.notification.domain.delivery.DeliveryStatus.RETRY_SCHEDULED;
import static kz.company.notification.domain.delivery.DeliveryStatus.SENT;
import static kz.company.notification.domain.delivery.DeliveryStatus.SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationStatusCalculatorTest {

    private static NotificationStatus calculate(DeliveryStatus... statuses) {
        return NotificationStatusCalculator.calculate(List.of(statuses));
    }

    @Test
    void calculatesAllOutcomes() {
        assertThat(calculate()).isEqualTo(NotificationStatus.ACCEPTED);
        assertThat(calculate(PENDING, SKIPPED)).isEqualTo(NotificationStatus.ACCEPTED);
        assertThat(calculate(PENDING, SENT)).isEqualTo(NotificationStatus.IN_PROGRESS);
        assertThat(calculate(RETRY_SCHEDULED)).isEqualTo(NotificationStatus.IN_PROGRESS);
        assertThat(calculate(SENT, DELIVERED, SKIPPED)).isEqualTo(NotificationStatus.COMPLETED);
        assertThat(calculate(SKIPPED, SKIPPED)).isEqualTo(NotificationStatus.COMPLETED);
        assertThat(calculate(SENT, FAILED)).isEqualTo(NotificationStatus.PARTIALLY_FAILED);
        assertThat(calculate(FAILED, SKIPPED)).isEqualTo(NotificationStatus.FAILED);
        assertThat(calculate(CANCELLED, CANCELLED)).isEqualTo(NotificationStatus.CANCELLED);
    }
}
