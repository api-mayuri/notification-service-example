package kz.company.notification.domain.preferences;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.template.LocaleTag;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Агрегат «Предпочтения получателя»: отписки по каналам и категориям, тихие часы,
 * часовой пояс (IANA) и язык по умолчанию.
 *
 * <p>Отписаться можно только от необязательных категорий ({@link Category#isOptional()}):
 * транзакционные и системные уведомления отписке не подлежат.
 */
public final class RecipientPreferences {

    private final RecipientId recipientId;
    private final Long version;
    private Set<ChannelType> unsubscribedChannels;
    private Set<Category> unsubscribedCategories;
    private QuietHours quietHours;
    private ZoneId timeZone;
    private LocaleTag locale;
    private Instant updatedAt;

    private RecipientPreferences(RecipientPreferencesSnapshot snapshot) {
        this.recipientId = Guard.notNull(snapshot.recipientId(), "recipientId");
        this.unsubscribedChannels = copyChannels(snapshot.unsubscribedChannels());
        this.unsubscribedCategories = copyCategories(snapshot.unsubscribedCategories());
        this.quietHours = snapshot.quietHours();
        this.timeZone = snapshot.timeZone() == null ? ZoneOffset.UTC : snapshot.timeZone();
        this.locale = snapshot.locale();
        this.updatedAt = snapshot.updatedAt();
        this.version = snapshot.version();
    }

    /** Предпочтения по умолчанию: без отписок и тихих часов, UTC. Не сохраняются, пока их не изменят. */
    public static RecipientPreferences defaults(RecipientId recipientId) {
        return new RecipientPreferences(new RecipientPreferencesSnapshot(recipientId, Set.of(), Set.of(), null,
                ZoneOffset.UTC, null, null, null));
    }

    public static RecipientPreferences restore(RecipientPreferencesSnapshot snapshot) {
        return new RecipientPreferences(snapshot);
    }

    public RecipientPreferencesSnapshot snapshot() {
        return new RecipientPreferencesSnapshot(recipientId, unsubscribedChannels, unsubscribedCategories, quietHours,
                timeZone, locale, updatedAt, version);
    }

    public void apply(PreferencesChange change, Instant now) {
        change.optionalUnsubscribedChannels().ifPresent(channels -> unsubscribedChannels = copyChannels(channels));
        change.optionalUnsubscribedCategories().ifPresent(categories -> unsubscribedCategories = copyCategories(categories));
        if (change.clearQuietHours()) {
            quietHours = null;
        } else if (change.quietHours() != null) {
            quietHours = change.quietHours();
        }
        if (change.timeZone() != null) {
            timeZone = change.timeZone();
        }
        if (change.clearLocale()) {
            locale = null;
        } else if (change.locale() != null) {
            locale = change.locale();
        }
        updatedAt = now;
    }

    /** Разрешена ли категория: обязательные категории разрешены всегда. */
    public boolean allowsCategory(Category category) {
        return !category.isOptional() || !unsubscribedCategories.contains(category);
    }

    /** Разрешён ли канал для категории: отписка от канала не действует на обязательные категории. */
    public boolean allowsChannel(ChannelType channel, Category category) {
        return !category.isOptional() || !unsubscribedChannels.contains(channel);
    }

    private static Set<ChannelType> copyChannels(Set<ChannelType> channels) {
        return channels == null ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(channels));
    }

    private static Set<Category> copyCategories(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Set.of();
        }
        for (Category category : categories) {
            if (!category.isOptional()) {
                throw new DomainValidationException("CATEGORY_NOT_UNSUBSCRIBABLE",
                        "Category " + category + " cannot be unsubscribed from");
            }
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(categories));
    }

    public RecipientId recipientId() {
        return recipientId;
    }

    public Set<ChannelType> unsubscribedChannels() {
        return unsubscribedChannels;
    }

    public Set<Category> unsubscribedCategories() {
        return unsubscribedCategories;
    }

    public Optional<QuietHours> quietHours() {
        return Optional.ofNullable(quietHours);
    }

    public ZoneId timeZone() {
        return timeZone;
    }

    public Optional<LocaleTag> locale() {
        return Optional.ofNullable(locale);
    }

    public Optional<Instant> updatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public Long version() {
        return version;
    }
}
