package kz.company.notification.infrastructure.out.channel.email;

import kz.company.notification.domain.delivery.SendResult;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Классификация ошибок SMTP: 4xx и сетевые — временные, 5xx — постоянные (RFC 5321, п. 4.2.1).
 * Код ищется в сообщениях цепочки причин: реализации Jakarta Mail включают ответ сервера в текст.
 */
public final class SmtpErrorClassifier {

    // Reply code only at line start or after ": " so ports/timeouts (e.g. "host:587") are not taken for 5xx.
    private static final Pattern REPLY_CODE = Pattern.compile("(?:^|: )([45])[0-9]{2}(?=[ \\-]|$)", Pattern.MULTILINE);

    private SmtpErrorClassifier() {
    }

    public static SendResult classify(MailException exception) {
        String reason = describe(exception);
        if (exception instanceof MailParseException || exception instanceof MailPreparationException) {
            return new SendResult.PermanentFailure("Invalid message: " + reason);
        }
        if (exception instanceof MailAuthenticationException) {
            return new SendResult.RetryableFailure("SMTP authentication failed: " + reason);
        }
        Integer replyClass = findReplyClass(exception);
        if (replyClass != null && replyClass == 5) {
            return new SendResult.PermanentFailure(reason);
        }
        return new SendResult.RetryableFailure(reason);
    }

    /** 4 или 5 — класс ответа SMTP; {@code null}, если кода нет (сетевая ошибка). */
    static Integer findReplyClass(Throwable exception) {
        Set<Throwable> seen = new HashSet<>();
        Throwable current = exception;
        while (current != null && seen.add(current)) {
            if (current.getMessage() != null) {
                Matcher matcher = REPLY_CODE.matcher(current.getMessage());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
            if (current instanceof org.springframework.mail.MailSendException sendException) {
                for (Exception failed : sendException.getFailedMessages().values()) {
                    Integer nested = findReplyClass(failed);
                    if (nested != null) {
                        return nested;
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private static String describe(Throwable exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() != null ? root.getMessage() : exception.getMessage();
        return root.getClass().getSimpleName() + ": " + (message == null ? "" : message.strip());
    }
}
