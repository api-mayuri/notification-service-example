package kz.company.notification.application.fake;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateRepository;
import kz.company.notification.domain.template.TemplateSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryTemplateRepository implements TemplateRepository {

    private final VersionedStore<TemplateId, TemplateSnapshot> store = new VersionedStore<>();

    @Override
    public Template save(Template template) {
        TemplateSnapshot s = template.snapshot();
        store.put(s.id(), s.version(), version -> new TemplateSnapshot(s.id(), s.key(), s.channel(), s.locale(),
                s.deduplicationWindow(), s.revisions(), s.createdAt(), s.updatedAt(), version));
        return Template.restore(store.get(s.id()).orElseThrow());
    }

    @Override
    public Optional<Template> find(TemplateKey key, ChannelType channel, LocaleTag locale) {
        return findAll().stream()
                .filter(t -> t.key().equals(key) && t.channel().equals(channel) && t.locale().equals(locale))
                .findFirst();
    }

    @Override
    public List<Template> findByKey(TemplateKey key) {
        return findAll().stream().filter(t -> t.key().equals(key)).toList();
    }

    @Override
    public List<Template> findAll() {
        List<Template> result = new ArrayList<>();
        store.values().forEach(snapshot -> result.add(Template.restore(snapshot)));
        return result;
    }
}
