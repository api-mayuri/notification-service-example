package kz.company.notification.infrastructure.in.messaging;

import kz.company.notification.application.port.in.DeliveryOutcome;
import kz.company.notification.application.port.in.ProcessDeliveryUseCase;
import kz.company.notification.domain.delivery.DeliveryId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelDeliveryListenerTest {

    private final ProcessDeliveryUseCase useCase = mock(ProcessDeliveryUseCase.class);
    private final ChannelDeliveryListener listener = new ChannelDeliveryListener("id", "topic", "group", 3, useCase,
            JsonMapper.builder().build());

    private static ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("notification.delivery.email.v1", 0, 0L, "user-1", value);
    }

    private static String message() {
        return "{\"deliveryId\":\"" + UUID.randomUUID() + "\",\"channel\":\"EMAIL\"}";
    }

    @Test
    void outcomesMapToBrokerBehaviour() {
        when(useCase.process(any(DeliveryId.class))).thenReturn(new DeliveryOutcome.RetryScheduled(Instant.EPOCH, "timeout"));
        assertThatThrownBy(() -> listener.onMessage(record(message()))).isInstanceOf(RetryableDeliveryException.class);

        when(useCase.process(any(DeliveryId.class))).thenReturn(new DeliveryOutcome.Failed("550", true));
        assertThatThrownBy(() -> listener.onMessage(record(message()))).isInstanceOf(DeliveryFailedException.class);

        when(useCase.process(any(DeliveryId.class))).thenReturn(new DeliveryOutcome.Sent("id", false));
        assertThatCode(() -> listener.onMessage(record(message()))).doesNotThrowAnyException();

        when(useCase.process(any(DeliveryId.class))).thenReturn(new DeliveryOutcome.Ignored("already sent"));
        assertThatCode(() -> listener.onMessage(record(message()))).doesNotThrowAnyException();
    }

    @Test
    void malformedMessagesAreInvalid() {
        assertThatThrownBy(() -> listener.onMessage(record("not json"))).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> listener.onMessage(record("{}"))).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> listener.onMessage(record(null))).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> listener.onMessage(record("{\"deliveryId\":\"x\"}")))
                .isInstanceOf(InvalidMessageException.class);
    }
}
