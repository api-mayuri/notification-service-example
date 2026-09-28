package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.delivery.ChannelType;


public class TemplateAlreadyExistsException extends DomainException {

    public TemplateAlreadyExistsException(TemplateKey key, ChannelType channel, LocaleTag locale) {
        super("TEMPLATE_ALREADY_EXISTS", DomainErrorKind.CONFLICT, "Template " + key + " for channel " + channel + " and locale " + locale + " already exists");
    }
}
