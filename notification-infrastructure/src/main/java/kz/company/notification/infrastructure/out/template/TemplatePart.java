package kz.company.notification.infrastructure.out.template;

import org.thymeleaf.templatemode.TemplateMode;

import java.util.UUID;

/**
 * Имя шаблона для Thymeleaf: {@code <templateId>:<version>:<part>:<mode>}. Версия шаблона
 * неизменна, поэтому разобранный шаблон можно кэшировать без проверки актуальности.
 */
record TemplatePart(UUID templateId, int version, Part part, TemplateMode mode) {

    enum Part {
        SUBJECT,
        BODY,
        TEXT_BODY
    }

    String templateName() {
        return templateId + ":" + version + ":" + part + ":" + mode;
    }

    static TemplatePart parse(String templateName) {
        String[] pieces = templateName.split(":");
        if (pieces.length != 4) {
            throw new IllegalArgumentException("Unexpected template name " + templateName);
        }
        return new TemplatePart(UUID.fromString(pieces[0]), Integer.parseInt(pieces[1]), Part.valueOf(pieces[2]),
                TemplateMode.valueOf(pieces[3]));
    }
}
