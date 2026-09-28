package kz.company.notification.application.service;

import kz.company.notification.application.port.in.DeliveryView;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.TemplateContent;

import java.util.Comparator;
import java.util.List;

/** Сборка представлений для ответов API из агрегатов. */
final class NotificationViewAssembler {

    private NotificationViewAssembler() {
    }

    static NotificationView toView(Notification notification, List<Delivery> deliveries) {
        String templateKey = notification.request().content() instanceof TemplateContent template
                ? template.templateKey().value()
                : null;
        return new NotificationView(
                notification.id().value(),
                notification.clientId().value(),
                notification.recipientId().value(),
                notification.status().name(),
                notification.category().name(),
                notification.priority().name(),
                notification.requestedChannels().stream().map(ChannelType::code).sorted().toList(),
                templateKey,
                notification.request().sendAt(),
                notification.metadata(),
                notification.createdAt(),
                notification.updatedAt(),
                deliveries.stream()
                        .sorted(Comparator.comparing(Delivery::channel))
                        .map(NotificationViewAssembler::toView)
                        .toList());
    }

    static DeliveryView toView(Delivery delivery) {
        return new DeliveryView(
                delivery.id().value(),
                delivery.channel().code(),
                delivery.status().name(),
                delivery.address().map(ChannelAddress::masked).orElse(null),
                delivery.attempts(),
                delivery.nextAttemptAt().orElse(null),
                delivery.lastError().orElse(null),
                delivery.providerMessageId().orElse(null),
                delivery.simulated(),
                delivery.skipReason().map(SkipReason::name).orElse(null),
                delivery.message().map(RenderedMessage::templateVersion).orElse(null),
                delivery.message().map(RenderedMessage::templateLocale).orElse(null),
                delivery.sentAt().orElse(null),
                delivery.updatedAt());
    }
}
