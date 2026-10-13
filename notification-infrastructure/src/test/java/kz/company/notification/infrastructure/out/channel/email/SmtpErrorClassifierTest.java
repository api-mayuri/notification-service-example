package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.domain.delivery.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpErrorClassifierTest {

    @Test
    void permanentFor5xxReplies() {
        MailSendException exception = new MailSendException("Failed messages",
                new jakarta.mail.MessagingException("550 5.1.1 <nobody@example.com>: Recipient address rejected"));

        assertThat(SmtpErrorClassifier.classify(exception)).isInstanceOf(SendResult.PermanentFailure.class);
    }

    @Test
    void retryableFor4xxReplies() {
        MailSendException exception = new MailSendException("451 4.7.1 Greylisted, try again later");

        assertThat(SmtpErrorClassifier.classify(exception)).isInstanceOf(SendResult.RetryableFailure.class);
    }

    @Test
    void codesInFailedMessagesAreInspected() {
        MailSendException exception = new MailSendException(Map.of(new Object(),
                new jakarta.mail.MessagingException("554 Relay access denied")));

        assertThat(SmtpErrorClassifier.findReplyClass(exception)).isEqualTo(5);
    }

    @Test
    void networkErrorsAreRetryable() {
        MailSendException exception = new MailSendException("Mail server connection failed",
                new jakarta.mail.MessagingException("Couldn't connect to host", new ConnectException("Connection refused")));

        assertThat(SmtpErrorClassifier.classify(exception)).isInstanceOf(SendResult.RetryableFailure.class);
    }

    @Test
    void portsAndTimeoutsAreNotTakenForReplyCodes() {
        MailSendException exception = new MailSendException("Mail server connection failed",
                new jakarta.mail.MessagingException("Couldn't connect to host, port: smtp.company.kz, 587; timeout 5000",
                        new ConnectException("Connection timed out: smtp.company.kz/10.0.0.1:587")));

        assertThat(SmtpErrorClassifier.findReplyClass(exception)).isNull();
        assertThat(SmtpErrorClassifier.classify(exception)).isInstanceOf(SendResult.RetryableFailure.class);
    }

    @Test
    void authenticationFailureIsRetryableAndBadMessageIsPermanent() {
        assertThat(SmtpErrorClassifier.classify(new MailAuthenticationException("535 Authentication failed")))
                .isInstanceOf(SendResult.RetryableFailure.class);
        assertThat(SmtpErrorClassifier.classify(new MailParseException("bad address")))
                .isInstanceOf(SendResult.PermanentFailure.class);
    }
}
