package kz.company.notification.infrastructure.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Строка таблицы {@code template} с версиями из {@code template_revision}. Версии — коллекция
 * embeddable-объектов: у них нет собственной идентичности вне шаблона.
 */
@Entity
@Table(name = "template")
public class TemplateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "locale", nullable = false, length = 35)
    private String locale;

    @Column(name = "deduplication_window_seconds")
    private Long deduplicationWindowSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "template_revision", joinColumns = @JoinColumn(name = "template_id"))
    @OrderBy("revisionNumber ASC")
    private List<TemplateRevisionEmbeddable> revisions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public Long getDeduplicationWindowSeconds() { return deduplicationWindowSeconds; }
    public void setDeduplicationWindowSeconds(Long seconds) { this.deduplicationWindowSeconds = seconds; }
    public List<TemplateRevisionEmbeddable> getRevisions() { return revisions; }
    public void setRevisions(List<TemplateRevisionEmbeddable> revisions) { this.revisions = revisions; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
