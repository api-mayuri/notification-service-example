package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;

import java.time.Duration;

/**
 * Изменить настройки шаблона.
 *
 * @param deduplicationWindow окно дедупликации; {@code null} — выключить дедупликацию
 */
public record UpdateTemplateSettingsCommand(TemplateKey key, ChannelType channel, LocaleTag locale,
                                            Duration deduplicationWindow) {
}
