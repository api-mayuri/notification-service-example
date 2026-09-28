package kz.company.notification.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Строка таблицы {@code notification}. Маппинг в домен — {@code NotificationMapper}. */
@Entity
@Table(name = "notification")
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 200)
    private String clientId;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(name = "recipient_id", nullable = false, length = 100)
    private String recipientId;

    @Column(name = "content_type", nullable = false, length = 16)
    private String contentType;

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "template_locale", length = 35)
    private String templateLocale;

    @Column(name = "template_variables", columnDefinition = "text")
    private String templateVariables;

    @Column(name = "raw_subject", columnDefinition = "text")
    private String rawSubject;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @Column(name = "raw_html", columnDefinition = "text")
    private String rawHtml;

    @Column(name = "channels", nullable = false, length = 500)
    private String channels;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "send_at")
    private Instant sendAt;

    @Column(name = "deduplication_key", length = 64)
    private String deduplicationKey;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getTemplateLocale() { return templateLocale; }
    public void setTemplateLocale(String templateLocale) { this.templateLocale = templateLocale; }
    public String getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(String templateVariables) { this.templateVariables = templateVariables; }
    public String getRawSubject() { return rawSubject; }
    public void setRawSubject(String rawSubject) { this.rawSubject = rawSubject; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getRawHtml() { return rawHtml; }
    public void setRawHtml(String rawHtml) { this.rawHtml = rawHtml; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSendAt() { return sendAt; }
    public void setSendAt(Instant sendAt) { this.sendAt = sendAt; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
