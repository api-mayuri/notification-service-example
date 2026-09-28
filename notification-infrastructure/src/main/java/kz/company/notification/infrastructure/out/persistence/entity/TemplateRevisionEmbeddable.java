package kz.company.notification.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Строка таблицы {@code template_revision}. */
@Embeddable
public class TemplateRevisionEmbeddable {

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "subject", columnDefinition = "text")
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "text_body", columnDefinition = "text")
    private String textBody;

    /** Имена обязательных переменных через запятую. */
    @Column(name = "required_variables", nullable = false, length = 2000)
    private String requiredVariables;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public int getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getTextBody() { return textBody; }
    public void setTextBody(String textBody) { this.textBody = textBody; }
    public String getRequiredVariables() { return requiredVariables; }
    public void setRequiredVariables(String requiredVariables) { this.requiredVariables = requiredVariables; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
