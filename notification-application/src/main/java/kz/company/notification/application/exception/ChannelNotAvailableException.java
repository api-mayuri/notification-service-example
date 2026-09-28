package kz.company.notification.application.exception;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;

import java.util.Set;

/** Запрошен канал, который не включён в этом развёртывании. */
public class ChannelNotAvailableException extends DomainValidationException {

    public ChannelNotAvailableException(Set<ChannelType> unavailable, Set<ChannelType> enabled) {
        super("CHANNEL_NOT_AVAILABLE", "Channels " + unavailable + " are not enabled; available: " + enabled);
    }
}
