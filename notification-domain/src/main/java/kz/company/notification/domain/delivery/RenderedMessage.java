package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.Optional;

/**
 * Готовое к отправке сообщение. Для шаблонных сообщений хранит ключ, версию и локаль шаблона,
 * по которым оно отрендерено.
 *
 * @param htmlBody HTML-версия; есть только у каналов с разметкой (email)
 */
public record RenderedMessage(
        String subject,
        String textBody,
        String htmlBody,
        String templateKey,
        Integer templateVersion,
        String templateLocale) {

    public RenderedMessage {
        if (isBlank(textBody) && isBlank(htmlBody)) {
            throw new DomainValidationException("EMPTY_MESSAGE", "Rendered message must have text or html body");
        }
    }

    public static RenderedMessage raw(String subject, String textBody, String htmlBody) {
        return new RenderedMessage(subject, textBody, htmlBody, null, null, null);
    }

    public Optional<String> optionalSubject() {
        return Optional.ofNullable(subject).filter(value -> !value.isBlank());
    }

    public Optional<String> optionalHtmlBody() {
        return Optional.ofNullable(htmlBody).filter(value -> !value.isBlank());
    }

    /** Текстовая версия; если её нет — HTML без тегов. */
    public String plainText() {
        if (!isBlank(textBody)) {
            return textBody;
        }
        return htmlBody.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</h[1-6]>|</li>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    public boolean isFromTemplate() {
        return templateKey != null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
