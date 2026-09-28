package kz.company.notification.infrastructure.out.channel.stub;

import kz.company.notification.domain.delivery.SendResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubProviderSupportTest {

    @Test
    void reliableStubReturnsSimulatedSuccess() {
        SendResult result = new StubProviderSupport(() -> 0.5)
                .simulate("SMS", UUID.randomUUID(), "+77***567", "text", StubBehaviour.RELIABLE);

        assertThat(result).isInstanceOf(SendResult.Success.class);
        assertThat(((SendResult.Success) result).simulated()).isTrue();
        assertThat(((SendResult.Success) result).providerMessageId()).startsWith("stub-sms-");
    }

    @Test
    void configuredFailuresAreSimulated() {
        StubBehaviour behaviour = new StubBehaviour(0.3, 0.2);

        assertThat(new StubProviderSupport(() -> 0.1).simulate("SMS", UUID.randomUUID(), "x", "t", behaviour))
                .isInstanceOf(SendResult.PermanentFailure.class);
        assertThat(new StubProviderSupport(() -> 0.4).simulate("SMS", UUID.randomUUID(), "x", "t", behaviour))
                .isInstanceOf(SendResult.RetryableFailure.class);
        assertThat(new StubProviderSupport(() -> 0.6).simulate("SMS", UUID.randomUUID(), "x", "t", behaviour))
                .isInstanceOf(SendResult.Success.class);
    }

    @Test
    void probabilitiesAreValidated() {
        assertThatThrownBy(() -> new StubBehaviour(0.8, 0.5)).isInstanceOf(IllegalArgumentException.class);
    }
}
