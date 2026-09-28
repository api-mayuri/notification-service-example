package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateKey;

import java.time.Duration;

/**
 * Создать шаблон (ключ + канал + локаль) с первой версией.
 *
 * @param activate сразу сделать первую версию активной
 */
public record CreateTemplateCommand(TemplateKey key, ChannelType channel, LocaleTag locale,
                                    TemplateContentDefinition content, Duration deduplicationWindow,
                                    boolean activate) {
}
