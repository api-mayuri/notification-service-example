package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.preferences.RecipientPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Выбор каналов: пересечение запрошенных каналов с предпочтениями получателя и доступными адресами.
 *
 * <ul>
 *     <li>Обязательные категории (TRANSACTIONAL, SYSTEM) нельзя отключить отпиской.</li>
 *     <li>Канал без контакта пропускается с причиной NO_CONTACT.</li>
 *     <li>Если все каналы пропущены из-за отсутствия контактов — это ошибка запроса.</li>
 * </ul>
 */
public final class ChannelSelectionPolicy {

    public ChannelSelection select(Set<ChannelType> requestedChannels, Category category,
                                   RecipientPreferences preferences, RecipientContacts contacts,
                                   ChannelAddressLookup addressLookup) {
        List<ChannelSelection.Selected> selected = new ArrayList<>();
        List<ChannelSelection.Skipped> skipped = new ArrayList<>();
        boolean categoryAllowed = preferences.allowsCategory(category);
        for (ChannelType channel : requestedChannels) {
            Optional<ChannelAddress> address = addressLookup.find(channel, contacts);
            if (address.isEmpty()) {
                skipped.add(new ChannelSelection.Skipped(channel, SkipReason.NO_CONTACT));
            } else if (!categoryAllowed) {
                skipped.add(new ChannelSelection.Skipped(channel, SkipReason.UNSUBSCRIBED_CATEGORY));
            } else if (!preferences.allowsChannel(channel, category)) {
                skipped.add(new ChannelSelection.Skipped(channel, SkipReason.UNSUBSCRIBED_CHANNEL));
            } else {
                selected.add(new ChannelSelection.Selected(channel, address.get()));
            }
        }
        boolean everyChannelLacksContact = !skipped.isEmpty()
                && skipped.stream().allMatch(skip -> skip.reason() == SkipReason.NO_CONTACT)
                && selected.isEmpty();
        if (everyChannelLacksContact) {
            throw new NoDeliverableChannelException(requestedChannels);
        }
        return new ChannelSelection(selected, skipped);
    }
}
