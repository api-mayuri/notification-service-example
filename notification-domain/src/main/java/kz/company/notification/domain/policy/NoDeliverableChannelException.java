package kz.company.notification.domain.policy;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;

import java.util.Set;

/** Ни для одного запрошенного канала нет контакта получателя. */
public class NoDeliverableChannelException extends DomainValidationException {

    public NoDeliverableChannelException(Set<ChannelType> channels) {
        super("NO_DELIVERABLE_CHANNEL", "Recipient has no contacts for requested channels " + channels);
    }
}
