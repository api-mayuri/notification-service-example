package kz.company.notification.application.settings;

import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.template.LocaleTag;

/** Общие настройки приёма уведомлений. */
public record NotificationSettings(LocaleTag defaultLocale) {

    public NotificationSettings {
        Guard.notNull(defaultLocale, "defaultLocale");
    }
}
