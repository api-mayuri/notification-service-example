package kz.company.notification.domain.template;

import kz.company.notification.domain.delivery.ChannelType;

import java.util.List;
import java.util.Optional;

/** Хранилище агрегатов {@link Template}. */
public interface TemplateRepository {

    Template save(Template template);

    Optional<Template> find(TemplateKey key, ChannelType channel, LocaleTag locale);

    List<Template> findByKey(TemplateKey key);

    List<Template> findAll();
}
