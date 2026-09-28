package kz.company.notification.infrastructure.out.channel.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import kz.company.notification.domain.delivery.SendResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Отправка через корпоративный SMTP-сервер ({@link JavaMailSenderImpl} из spring-boot-starter-mail).
 * Письмо multipart/alternative: текст и HTML, UTF-8, заголовки Message-ID и List-Unsubscribe.
 */
@Component
@ConditionalOnProperty(prefix = "notification.channels.email", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSenderImpl mailSender;
    private final EmailChannelProperties properties;

    public SmtpEmailProvider(JavaMailSenderImpl mailSender, EmailChannelProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public SendResult send(EmailMessage email) {
        String messageId = "<" + email.deliveryId() + "@" + properties.messageIdDomain() + ">";
        try {
            MimeMessage mimeMessage = build(email, messageId);
            mailSender.send(mimeMessage);
            return SendResult.Success.real(messageId);
        } catch (MailException exception) {
            return SmtpErrorClassifier.classify(exception);
        }
    }

    private MimeMessage build(EmailMessage email, String messageId) {
        try {
            MimeMessage mimeMessage = new FixedMessageIdMimeMessage(mailSender.getSession(), messageId);
            boolean hasHtml = email.htmlBody() != null && !email.htmlBody().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasHtml, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.fromName());
            if (properties.replyTo() != null && !properties.replyTo().isBlank()) {
                helper.setReplyTo(properties.replyTo());
            }
            helper.setTo(email.to().value());
            helper.setSubject(email.subject() == null ? "" : email.subject());
            if (hasHtml) {
                helper.setText(email.textBody(), email.htmlBody());
            } else {
                helper.setText(email.textBody(), false);
            }
            if (email.unsubscribeUrl() != null) {
                mimeMessage.setHeader("List-Unsubscribe", "<" + email.unsubscribeUrl() + ">");
                mimeMessage.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            }
            return mimeMessage;
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Could not build email " + email.deliveryId(), exception);
        }
    }

    /** MimeMessage, который не перезаписывает Message-ID при сохранении: id = id доставки. */
    static final class FixedMessageIdMimeMessage extends MimeMessage {

        private final String messageId;

        FixedMessageIdMimeMessage(Session session, String messageId) {
            super(session);
            this.messageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
        }
    }
}
