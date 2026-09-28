package kz.company.notification.domain.delivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryStatusTest {

    @Test
    void terminalStatusesHaveNoTransitionsExceptSentToDelivered() {
        assertThat(DeliveryStatus.FAILED.allowedTargets()).isEmpty();
        assertThat(DeliveryStatus.SKIPPED.allowedTargets()).isEmpty();
        assertThat(DeliveryStatus.CANCELLED.allowedTargets()).isEmpty();
        assertThat(DeliveryStatus.DELIVERED.allowedTargets()).isEmpty();
        assertThat(DeliveryStatus.SENT.canTransitionTo(DeliveryStatus.DELIVERED)).isTrue();
        assertThat(DeliveryStatus.SENT.isTerminal()).isTrue();
    }

    @Test
    void pendingCanStartSendingButNotBeSentDirectly() {
        assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.SENDING)).isTrue();
        assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.SENT)).isFalse();
        assertThat(DeliveryStatus.RETRY_SCHEDULED.canTransitionTo(DeliveryStatus.SENDING)).isTrue();
    }

    @Test
    void awaitingSendAndSuccessFlags() {
        assertThat(DeliveryStatus.PENDING.isAwaitingSend()).isTrue();
        assertThat(DeliveryStatus.RETRY_SCHEDULED.isAwaitingSend()).isTrue();
        assertThat(DeliveryStatus.SENDING.isAwaitingSend()).isFalse();
        assertThat(DeliveryStatus.DELIVERED.isSuccessful()).isTrue();
        assertThat(DeliveryStatus.FAILED.isSuccessful()).isFalse();
    }
}
