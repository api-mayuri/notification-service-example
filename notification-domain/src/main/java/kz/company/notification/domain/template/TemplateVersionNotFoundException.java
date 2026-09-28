package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.delivery.ChannelType;


public class TemplateVersionNotFoundException extends DomainException {

    public TemplateVersionNotFoundException(TemplateKey key, ChannelType channel, LocaleTag locale, TemplateVersion version) {
        super("TEMPLATE_VERSION_NOT_FOUND", DomainErrorKind.NOT_FOUND, "Template " + key + "/" + channel + "/" + locale + " has no version " + version);
    }
}
