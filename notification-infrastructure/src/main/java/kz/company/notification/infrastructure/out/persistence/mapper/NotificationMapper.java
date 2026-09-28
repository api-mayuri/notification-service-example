package kz.company.notification.infrastructure.out.persistence.mapper;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.DeduplicationKey;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationContent;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationRequest;
import kz.company.notification.domain.notification.NotificationSnapshot;
import kz.company.notification.domain.notification.NotificationStatus;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RawContent;
import kz.company.notification.domain.notification.Recipient;
import kz.company.notification.domain.notification.RecipientContacts;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.notification.TemplateContent;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.infrastructure.out.persistence.entity.NotificationJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Маппинг {@link Notification} ↔ {@link NotificationJpaEntity}. Контакты получателя в уведомлении
 * не хранятся: адреса лежат только в доставках (меньше копий персональных данных).
 */
@Component
public class NotificationMapper {

    private static final String TEMPLATE = "TEMPLATE";
    private static final String RAW = "RAW";

    private final JsonText jsonText;

    public NotificationMapper(JsonText jsonText) {
        this.jsonText = jsonText;
    }

    public NotificationJpaEntity toEntity(Notification notification) {
        NotificationSnapshot snapshot = notification.snapshot();
        NotificationRequest request = snapshot.request();
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(snapshot.id().value());
        entity.setClientId(request.clientId().value());
        entity.setIdempotencyKey(request.idempotencyKey() == null ? null : request.idempotencyKey().value());
        entity.setRequestFingerprint(request.requestFingerprint());
        entity.setRecipientId(request.recipientId().value());
        switch (request.content()) {
            case TemplateContent template -> {
                entity.setContentType(TEMPLATE);
                entity.setTemplateKey(template.templateKey().value());
                entity.setTemplateLocale(template.locale() == null ? null : template.locale().value());
                entity.setTemplateVariables(jsonText.write(template.variables()));
            }
            case RawContent raw -> {
                entity.setContentType(RAW);
                entity.setRawSubject(raw.subject());
                entity.setRawText(raw.text());
                entity.setRawHtml(raw.html());
            }
        }
        entity.setChannels(DelimitedText.join(request.channels(), ChannelType::code));
        entity.setCategory(request.category().name());
        entity.setPriority(request.priority().name());
        entity.setStatus(snapshot.status().name());
        entity.setSendAt(request.sendAt());
        entity.setDeduplicationKey(snapshot.deduplicationKey() == null ? null : snapshot.deduplicationKey().value());
        entity.setMetadata(jsonText.write(request.metadata()));
        entity.setCreatedAt(snapshot.createdAt());
        entity.setUpdatedAt(snapshot.updatedAt());
        entity.setVersion(snapshot.version());
        return entity;
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        NotificationContent content = TEMPLATE.equals(entity.getContentType())
                ? new TemplateContent(new TemplateKey(entity.getTemplateKey()),
                entity.getTemplateLocale() == null ? null : LocaleTag.of(entity.getTemplateLocale()),
                jsonText.readObjectMap(entity.getTemplateVariables()))
                : new RawContent(entity.getRawSubject(), entity.getRawText(), entity.getRawHtml());
        NotificationRequest request = new NotificationRequest(
                new ClientId(entity.getClientId()),
                entity.getIdempotencyKey() == null ? null : new IdempotencyKey(entity.getIdempotencyKey()),
                entity.getRequestFingerprint(),
                new Recipient(new RecipientId(entity.getRecipientId()), RecipientContacts.NONE),
                content,
                DelimitedText.split(entity.getChannels(), ChannelType::of),
                Category.valueOf(entity.getCategory()),
                Priority.valueOf(entity.getPriority()),
                entity.getSendAt(),
                jsonText.readStringMap(entity.getMetadata()));
        return Notification.restore(new NotificationSnapshot(
                new NotificationId(entity.getId()),
                request,
                entity.getDeduplicationKey() == null ? null : new DeduplicationKey(entity.getDeduplicationKey()),
                NotificationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }
}
