package kz.company.notification.application.port.in;

/** Результат предпросмотра. */
public record TemplatePreview(String subject, String textBody, String htmlBody, int version, String locale) {
}
