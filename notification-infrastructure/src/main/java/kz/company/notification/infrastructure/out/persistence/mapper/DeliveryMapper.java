package kz.company.notification.infrastructure.out.persistence.mapper;

import kz.company.notification.application.port.out.ChannelSenderRegistry;
import kz.company.notification.domain.common.DomainException;
import kz.company.notification.domain.delivery.ChannelAddress;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.Delivery;
import kz.company.notification.domain.delivery.DeliveryId;
import kz.company.notification.domain.delivery.DeliverySnapshot;
import kz.company.notification.domain.delivery.DeliveryStatus;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.delivery.SkipReason;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.infrastructure.out.persistence.entity.DeliveryJpaEntity;
import org.springframework.stereotype.Component;

/** Маппинг {@link Delivery} ↔ {@link DeliveryJpaEntity}. Адрес восстанавливается через реестр каналов. */
@Component
public class DeliveryMapper {

    private final ChannelSenderRegistry channelRegistry;

    public DeliveryMapper(ChannelSenderRegistry channelRegistry) {
        this.channelRegistry = channelRegistry;
    }

    public DeliveryJpaEntity toEntity(Delivery delivery) {
        DeliverySnapshot s = delivery.snapshot();
        DeliveryJpaEntity entity = new DeliveryJpaEntity();
        entity.setId(s.id().value());
        entity.setNotificationId(s.notificationId().value());
        entity.setRecipientId(s.recipientId().value());
        entity.setChannel(s.channel().code());
        entity.setAddress(s.address() == null ? null : s.address().value());
        if (s.message() != null) {
            entity.setSubject(s.message().subject());
            entity.setTextBody(s.message().textBody());
            entity.setHtmlBody(s.message().htmlBody());
            entity.setTemplateKey(s.message().templateKey());
            entity.setTemplateVersion(s.message().templateVersion());
            entity.setTemplateLocale(s.message().templateLocale());
        }
        entity.setCategory(s.category().name());
        entity.setPriority(s.priority().name());
        entity.setStatus(s.status().name());
        entity.setAttempts(s.attempts());
        entity.setNextAttemptAt(s.nextAttemptAt());
        entity.setDispatchedAt(s.dispatchedAt());
        entity.setLastError(s.lastError());
        entity.setProviderMessageId(s.providerMessageId());
        entity.setSimulated(s.simulated());
        entity.setSkipReason(s.skipReason() == null ? null : s.skipReason().name());
        entity.setSentAt(s.sentAt());
        entity.setCreatedAt(s.createdAt());
        entity.setUpdatedAt(s.updatedAt());
        entity.setVersion(s.version());
        return entity;
    }

    public Delivery toDomain(DeliveryJpaEntity entity) {
        ChannelType channel = ChannelType.of(entity.getChannel());
        boolean hasMessage = entity.getTextBody() != null || entity.getHtmlBody() != null;
        RenderedMessage message = hasMessage
                ? new RenderedMessage(entity.getSubject(), entity.getTextBody(), entity.getHtmlBody(),
                entity.getTemplateKey(), entity.getTemplateVersion(), entity.getTemplateLocale())
                : null;
        return Delivery.restore(new DeliverySnapshot(
                new DeliveryId(entity.getId()),
                new NotificationId(entity.getNotificationId()),
                new RecipientId(entity.getRecipientId()),
                channel,
                entity.getAddress() == null ? null : restoreAddress(channel, entity.getAddress()),
                message,
                Category.valueOf(entity.getCategory()),
                Priority.valueOf(entity.getPriority()),
                DeliveryStatus.valueOf(entity.getStatus()),
                entity.getAttempts(),
                entity.getNextAttemptAt(),
                entity.getDispatchedAt(),
                entity.getLastError(),
                entity.getProviderMessageId(),
                entity.isSimulated(),
                entity.getSkipReason() == null ? null : SkipReason.valueOf(entity.getSkipReason()),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }

    private ChannelAddress restoreAddress(ChannelType channel, String value) {
        try {
            return channelRegistry.find(channel)
                    .map(sender -> sender.parseAddress(value))
                    .orElseGet(() -> new StoredChannelAddress(value));
        } catch (DomainException invalidNow) {
            return new StoredChannelAddress(value);
        }
    }
}
