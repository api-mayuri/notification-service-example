package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.delivery.ChannelType;


public class TemplateNotFoundException extends DomainException {

    public TemplateNotFoundException(TemplateKey key) {
        super("TEMPLATE_NOT_FOUND", DomainErrorKind.NOT_FOUND, "Template " + key + " not found");
    }

    public TemplateNotFoundException(TemplateKey key, ChannelType channel, LocaleTag locale) {
        super("TEMPLATE_NOT_FOUND", DomainErrorKind.NOT_FOUND, "Template " + key + " for channel " + channel + " and locale " + locale + " not found");
    }
}
