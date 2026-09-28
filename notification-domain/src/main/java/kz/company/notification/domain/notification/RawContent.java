package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;

/**
 * Готовое содержимое без шаблона. Разрешено только доверенным отправителям
 * (scope {@code notifications:send-raw}); проверка — в сценарии отправки.
 */
public record RawContent(String subject, String text, String html) implements NotificationContent {

    public static final int MAX_SUBJECT_LENGTH = 998;
    public static final int MAX_BODY_LENGTH = 1_000_000;

    public RawContent {
        Guard.maxLength(subject, MAX_SUBJECT_LENGTH, "subject");
        Guard.maxLength(text, MAX_BODY_LENGTH, "text");
        Guard.maxLength(html, MAX_BODY_LENGTH, "html");
        if (isBlank(text) && isBlank(html)) {
            throw new DomainValidationException("EMPTY_CONTENT", "Raw content must contain text or html");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
