package kz.company.notification.application.port.in;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;

/** Активировать версию шаблона; прежняя активная архивируется, кэш рендера сбрасывается. */
public record ActivateTemplateVersionCommand(TemplateKey key, ChannelType channel, LocaleTag locale,
                                             TemplateVersion version) {
}
