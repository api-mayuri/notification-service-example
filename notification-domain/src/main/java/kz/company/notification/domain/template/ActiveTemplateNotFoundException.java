package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainErrorKind;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.delivery.ChannelType;

import java.util.List;

public class ActiveTemplateNotFoundException extends DomainException {

    public ActiveTemplateNotFoundException(TemplateKey key, ChannelType channel, List<LocaleTag> triedLocales) {
        super("TEMPLATE_NOT_AVAILABLE", DomainErrorKind.VALIDATION, "No active template " + key + " for channel " + channel + ", tried locales " + triedLocales);
    }
}
