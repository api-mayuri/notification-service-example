package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderedMessageTest {

    @Test
    void plainTextFallsBackToStrippedHtml() {
        RenderedMessage message = RenderedMessage.raw("Hi", null,
                "<html><style>p{}</style><body><p>Hello <b>Anna</b></p><p>Bye</p></body></html>");

        assertThat(message.plainText()).isEqualTo("Hello Anna\nBye");
        assertThat(message.isFromTemplate()).isFalse();
    }

    @Test
    void textBodyWinsOverHtml() {
        RenderedMessage message = new RenderedMessage(null, "text", "<p>html</p>", "welcome", 2, "ru");

        assertThat(message.plainText()).isEqualTo("text");
        assertThat(message.optionalSubject()).isEmpty();
        assertThat(message.isFromTemplate()).isTrue();
    }

    @Test
    void emptyMessageIsRejected() {
        assertThatThrownBy(() -> RenderedMessage.raw("subject", " ", null))
                .isInstanceOf(DomainValidationException.class);
    }
}
