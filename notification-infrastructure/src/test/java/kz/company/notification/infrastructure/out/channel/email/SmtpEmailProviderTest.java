package kz.company.notification.infrastructure.out.channel.email;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import kz.company.notification.domain.delivery.EmailAddress;
import kz.company.notification.domain.delivery.SendResult;
import kz.company.notification.infrastructure.out.channel.stub.StubBehaviour;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpEmailProviderTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private SmtpEmailProvider provider(int port) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(port);
        sender.setDefaultEncoding("UTF-8");
        return new SmtpEmailProvider(sender, new EmailChannelProperties(true, "smtp", 5, "no-reply@example.com",
                "Уведомления", "support@example.com", "notification.test", null, StubBehaviour.RELIABLE));
    }

    @Test
    void sendsMultipartEmailWithMessageIdAndUnsubscribeHeader() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        SendResult result = provider(greenMail.getSmtp().getPort()).send(new EmailMessage(deliveryId, "user-1",
                new EmailAddress("user@example.com"), "Заказ отправлен", "Текст", "<p>HTML</p>",
                "https://example.com/unsubscribe?r=user-1"));

        assertThat(result).isEqualTo(SendResult.Success.real("<" + deliveryId + "@notification.test>"));
        MimeMessage received = greenMail.getReceivedMessages()[0];
        assertThat(received.getSubject()).isEqualTo("Заказ отправлен");
        assertThat(received.getMessageID()).isEqualTo("<" + deliveryId + "@notification.test>");
        assertThat(received.getHeader("List-Unsubscribe")[0]).isEqualTo("<https://example.com/unsubscribe?r=user-1>");
        assertThat(received.getContentType()).startsWith("multipart/");
        assertThat(received.getReplyTo()[0].toString()).isEqualTo("support@example.com");
    }

    @Test
    void unreachableServerIsTemporaryFailure() {
        SendResult result = provider(1).send(new EmailMessage(UUID.randomUUID(), "user-1",
                new EmailAddress("user@example.com"), "s", "text", null, null));

        assertThat(result).isInstanceOf(SendResult.RetryableFailure.class);
    }
}
