package kz.company.notification.domain.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonTest {

    @Test
    void maskingKeepsEdges() {
        assertThat(Masking.keepEdges("abcdef", 3)).isEqualTo("******");
        assertThat(Masking.keepEdges("", 3)).isEqualTo("");
        assertThat(Masking.email("ab@example.com")).isEqualTo("**@example.com");
        assertThat(Masking.email("broken")).isEqualTo("b***n");
    }

    @Test
    void guardsReportFieldInErrorCode() {
        assertThatThrownBy(() -> Guard.notBlank(" ", "clientId"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("clientId");
        DomainValidationException exception = DomainValidationException.invalidValue("templateKey", "bad");
        assertThat(exception.errorCode()).isEqualTo("INVALID_TEMPLATE_KEY");
        assertThat(exception.kind()).isEqualTo(DomainErrorKind.VALIDATION);
        assertThatThrownBy(() -> Guard.notEmpty(List.of(), "channels")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Guard.positive(0, "limit")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Guard.notNull(null, "value")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void aggregateEventsArePulledOnce() {
        AggregateEvents events = new AggregateEvents();
        DomainEvent event = () -> Instant.EPOCH;
        events.register(event);

        assertThat(events.pull()).containsExactly(event);
        assertThat(events.pull()).isEmpty();
    }
}
