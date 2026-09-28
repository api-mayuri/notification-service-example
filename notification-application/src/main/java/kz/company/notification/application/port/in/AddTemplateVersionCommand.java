package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateKey;

/** Добавить новую версию (DRAFT) к существующему шаблону. */
public record AddTemplateVersionCommand(TemplateKey key, ChannelType channel, LocaleTag locale,
                                        TemplateContentDefinition content, boolean activate) {
}
