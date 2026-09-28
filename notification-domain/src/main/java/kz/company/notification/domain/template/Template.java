package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Агрегат «Шаблон»: ключ + канал + локаль и список версий, из которых активна не больше одной.
 */
public final class Template {

    public static final Duration MAX_DEDUPLICATION_WINDOW = Duration.ofDays(1);

    private final TemplateId id;
    private final TemplateKey key;
    private final ChannelType channel;
    private final LocaleTag locale;
    private final List<TemplateRevision> revisions;
    private final Instant createdAt;
    private final Long version;
    private Duration deduplicationWindow;
    private Instant updatedAt;

    private Template(TemplateSnapshot snapshot) {
        this.id = Guard.notNull(snapshot.id(), "templateId");
        this.key = Guard.notNull(snapshot.key(), "templateKey");
        this.channel = Guard.notNull(snapshot.channel(), "channel");
        this.locale = Guard.notNull(snapshot.locale(), "locale");
        this.revisions = new ArrayList<>(Guard.notEmpty(snapshot.revisions(), "revisions"));
        this.revisions.sort(Comparator.comparing(TemplateRevision::version));
        this.deduplicationWindow = validateWindow(snapshot.deduplicationWindow());
        this.createdAt = Guard.notNull(snapshot.createdAt(), "createdAt");
        this.updatedAt = Guard.notNull(snapshot.updatedAt(), "updatedAt");
        this.version = snapshot.version();
        long active = revisions.stream().filter(revision -> revision.status() == TemplateStatus.ACTIVE).count();
        if (active > 1) {
            throw new IllegalArgumentException("Template " + id + " has more than one active version");
        }
    }

    /** Новый шаблон с первой версией в статусе DRAFT. */
    public static Template create(TemplateId id, TemplateKey key, ChannelType channel, LocaleTag locale,
                                  TemplateContentDefinition content, Duration deduplicationWindow, Instant now) {
        TemplateRevision first = new TemplateRevision(TemplateVersion.FIRST, content, TemplateStatus.DRAFT, now);
        return new Template(new TemplateSnapshot(id, key, channel, locale, deduplicationWindow, List.of(first),
                now, now, null));
    }

    public static Template restore(TemplateSnapshot snapshot) {
        return new Template(snapshot);
    }

    public TemplateSnapshot snapshot() {
        return new TemplateSnapshot(id, key, channel, locale, deduplicationWindow, List.copyOf(revisions),
                createdAt, updatedAt, version);
    }

    /** Добавить новую версию в статусе DRAFT. @return номер новой версии */
    public TemplateVersion addRevision(TemplateContentDefinition content, Instant now) {
        TemplateVersion next = latestRevision().version().next();
        revisions.add(new TemplateRevision(next, content, TemplateStatus.DRAFT, now));
        updatedAt = now;
        return next;
    }

    /** Активировать версию; прежняя активная версия архивируется. */
    public void activate(TemplateVersion versionToActivate, Instant now) {
        TemplateRevision target = revision(versionToActivate)
                .orElseThrow(() -> new TemplateVersionNotFoundException(key, channel, locale, versionToActivate));
        if (target.status() == TemplateStatus.ACTIVE) {
            return;
        }
        revisions.replaceAll(revision -> {
            if (revision.version().equals(versionToActivate)) {
                return revision.withStatus(TemplateStatus.ACTIVE);
            }
            return revision.status() == TemplateStatus.ACTIVE ? revision.withStatus(TemplateStatus.ARCHIVED) : revision;
        });
        updatedAt = now;
    }

    public void changeDeduplicationWindow(Duration window, Instant now) {
        deduplicationWindow = validateWindow(window);
        updatedAt = now;
    }

    public Optional<TemplateRevision> activeRevision() {
        return revisions.stream().filter(revision -> revision.status() == TemplateStatus.ACTIVE).findFirst();
    }

    public Optional<TemplateRevision> revision(TemplateVersion revisionVersion) {
        return revisions.stream().filter(revision -> revision.version().equals(revisionVersion)).findFirst();
    }

    public TemplateRevision latestRevision() {
        return revisions.getLast();
    }

    private static Duration validateWindow(Duration window) {
        if (window == null) {
            return null;
        }
        if (window.isNegative() || window.isZero() || window.compareTo(MAX_DEDUPLICATION_WINDOW) > 0) {
            throw DomainValidationException.invalidValue("deduplicationWindow", "must be between 1 second and 1 day");
        }
        return window;
    }

    public TemplateId id() {
        return id;
    }

    public TemplateKey key() {
        return key;
    }

    public ChannelType channel() {
        return channel;
    }

    public LocaleTag locale() {
        return locale;
    }

    public List<TemplateRevision> revisions() {
        return List.copyOf(revisions);
    }

    public Optional<Duration> deduplicationWindow() {
        return Optional.ofNullable(deduplicationWindow);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
    }
}
