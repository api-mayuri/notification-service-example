package kz.company.notification.domain.delivery;

import kz.company.notification.domain.common.Guard;

import java.time.Instant;

/**
 * План доставки по одному каналу: результат выбора канала, рендера и расчёта времени отправки.
 */
public sealed interface DeliveryPlan {

    ChannelType channel();

    /** Доставку нужно выполнить не раньше {@code scheduledFor}. */
    record Deliverable(ChannelType channel, ChannelAddress address, RenderedMessage message, Instant scheduledFor)
            implements DeliveryPlan {
        public Deliverable {
            Guard.notNull(channel, "channel");
            Guard.notNull(address, "address");
            Guard.notNull(message, "message");
            Guard.notNull(scheduledFor, "scheduledFor");
        }
    }

    /** Доставку нужно пропустить. */
    record Skip(ChannelType channel, SkipReason reason) implements DeliveryPlan {
        public Skip {
            Guard.notNull(channel, "channel");
            Guard.notNull(reason, "reason");
        }
    }
}
