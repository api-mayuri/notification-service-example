package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.RecipientContacts;

import java.util.Optional;

/**
 * Поиск адреса получателя для канала среди его контактов. Реализуется реестром каналов:
 * каждый канал знает, какой контакт ему нужен и как его проверить.
 */
@FunctionalInterface
public interface ChannelAddressLookup {

    /**
     * @return адрес или пусто, если нужного контакта нет
     * @throws kz.company.notification.domain.common.DomainValidationException если контакт есть, но неверного формата
     */
    Optional<ChannelAddress> find(ChannelType channel, RecipientContacts contacts);
}
