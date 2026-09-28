package kz.company.notification.application.port.out;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.policy.ChannelAddressLookup;

import java.util.Optional;
import java.util.Set;

/**
 * Реестр отправителей (Strategy + Registry): выбор {@link ChannelSender} по каналу без switch.
 * Содержит только включённые каналы.
 */
public interface ChannelSenderRegistry extends ChannelAddressLookup {

    Set<ChannelType> enabledChannels();

    Optional<ChannelSender> find(ChannelType channel);

    default boolean isEnabled(ChannelType channel) {
        return enabledChannels().contains(channel);
    }

    @Override
    default Optional<ChannelAddress> find(ChannelType channel, RecipientContacts contacts) {
        return find(channel).flatMap(sender -> contacts.find(sender.contactType()).map(sender::parseAddress));
    }
}
