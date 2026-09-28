package kz.company.notification.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalTime;

/** Строка таблицы {@code recipient_preferences}. */
@Entity
@Table(name = "recipient_preferences")
public class RecipientPreferencesJpaEntity {

    @Id
    @Column(name = "recipient_id", length = 100)
    private String recipientId;

    /** Коды каналов через запятую. */
    @Column(name = "unsubscribed_channels", nullable = false, length = 500)
    private String unsubscribedChannels;

    /** Категории через запятую. */
    @Column(name = "unsubscribed_categories", nullable = false, length = 200)
    private String unsubscribedCategories;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "locale", length = 35)
    private String locale;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getUnsubscribedChannels() { return unsubscribedChannels; }
    public void setUnsubscribedChannels(String unsubscribedChannels) { this.unsubscribedChannels = unsubscribedChannels; }
    public String getUnsubscribedCategories() { return unsubscribedCategories; }
    public void setUnsubscribedCategories(String categories) { this.unsubscribedCategories = categories; }
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(LocalTime quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(LocalTime quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
