package kz.company.notification.domain.template;

import kz.company.notification.domain.common.Guard;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Версия шаблона — сущность внутри агрегата {@link Template}. Содержимое версии неизменно;
 * меняется только статус.
 */
public record TemplateRevision(TemplateVersion version, TemplateContentDefinition content,
                               TemplateStatus status, Instant createdAt) {

    public TemplateRevision {
        Guard.notNull(version, "version");
        Guard.notNull(content, "content");
        Guard.notNull(status, "status");
        Guard.notNull(createdAt, "createdAt");
    }

    TemplateRevision withStatus(TemplateStatus newStatus) {
        return new TemplateRevision(version, content, newStatus, createdAt);
    }

    /** Обязательные переменные, которых нет среди переданных (значение {@code null} считается отсутствием). */
    public Set<String> missingVariables(Map<String, Object> variables) {
        Set<String> missing = new TreeSet<>();
        for (String name : content.requiredVariables()) {
            if (variables == null || variables.get(name) == null) {
                missing.add(name);
            }
        }
        return missing;
    }

    /** @throws MissingTemplateVariablesException если не хватает обязательных переменных */
    public void requireVariables(TemplateKey key, Map<String, Object> variables) {
        Set<String> missing = missingVariables(variables);
        if (!missing.isEmpty()) {
            throw new MissingTemplateVariablesException(key, missing);
        }
    }
}
