package kz.company.notification.domain.policy;

import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.SkipReason;

import java.util.List;

/**
 * Результат {@link ChannelSelectionPolicy}: каналы, по которым нужно отправлять, и пропущенные каналы.
 */
public record ChannelSelection(List<Selected> selected, List<Skipped> skipped) {

    public ChannelSelection {
        selected = List.copyOf(selected);
        skipped = List.copyOf(skipped);
    }

    public record Selected(ChannelType channel, ChannelAddress address) {
    }

    public record Skipped(ChannelType channel, SkipReason reason) {
    }
}
