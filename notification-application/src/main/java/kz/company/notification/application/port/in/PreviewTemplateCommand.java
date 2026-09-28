package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;

import java.util.Map;

/**
 * Предпросмотр: рендер с тестовыми переменными без отправки.
 *
 * @param version версия; {@code null} — активная, а если активной нет — последняя
 */
public record PreviewTemplateCommand(TemplateKey key, ChannelType channel, LocaleTag locale,
                                     TemplateVersion version, Map<String, Object> variables) {

    public PreviewTemplateCommand {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
