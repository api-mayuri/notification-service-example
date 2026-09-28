package kz.company.notification.infrastructure.out.persistence.mapper;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.domain.preferences.RecipientPreferencesSnapshot;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.infrastructure.out.persistence.entity.RecipientPreferencesJpaEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/** Маппинг {@link RecipientPreferences} ↔ {@link RecipientPreferencesJpaEntity}. */
@Component
public class PreferencesMapper {

    public RecipientPreferencesJpaEntity toEntity(RecipientPreferences preferences) {
        RecipientPreferencesSnapshot s = preferences.snapshot();
        RecipientPreferencesJpaEntity entity = new RecipientPreferencesJpaEntity();
        entity.setRecipientId(s.recipientId().value());
        entity.setUnsubscribedChannels(DelimitedText.join(s.unsubscribedChannels(), ChannelType::code));
        entity.setUnsubscribedCategories(DelimitedText.join(s.unsubscribedCategories(), Category::name));
        entity.setQuietHoursStart(s.quietHours() == null ? null : s.quietHours().start());
        entity.setQuietHoursEnd(s.quietHours() == null ? null : s.quietHours().end());
        entity.setTimeZone(s.timeZone().getId());
        entity.setLocale(s.locale() == null ? null : s.locale().value());
        entity.setUpdatedAt(s.updatedAt());
        entity.setVersion(s.version());
        return entity;
    }

    public RecipientPreferences toDomain(RecipientPreferencesJpaEntity entity) {
        QuietHours quietHours = entity.getQuietHoursStart() == null || entity.getQuietHoursEnd() == null
                ? null : new QuietHours(entity.getQuietHoursStart(), entity.getQuietHoursEnd());
        return RecipientPreferences.restore(new RecipientPreferencesSnapshot(
                new RecipientId(entity.getRecipientId()),
                DelimitedText.split(entity.getUnsubscribedChannels(), ChannelType::of),
                DelimitedText.split(entity.getUnsubscribedCategories(), Category::valueOf),
                quietHours,
                ZoneId.of(entity.getTimeZone()),
                entity.getLocale() == null ? null : LocaleTag.of(entity.getLocale()),
                entity.getUpdatedAt(),
                entity.getVersion()));
    }
}
