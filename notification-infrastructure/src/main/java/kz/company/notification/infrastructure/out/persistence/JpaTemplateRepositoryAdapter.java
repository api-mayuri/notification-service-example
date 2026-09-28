package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateRepository;
import kz.company.notification.infrastructure.out.persistence.mapper.TemplateMapper;
import kz.company.notification.infrastructure.out.persistence.repository.TemplateJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Хранилище шаблонов на JPA. */
@Repository
public class JpaTemplateRepositoryAdapter implements TemplateRepository {

    private final TemplateJpaRepository jpaRepository;
    private final TemplateMapper mapper;

    public JpaTemplateRepositoryAdapter(TemplateJpaRepository jpaRepository, TemplateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Template save(Template template) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(template)));
    }

    @Override
    public Optional<Template> find(TemplateKey key, ChannelType channel, LocaleTag locale) {
        return jpaRepository.findByTemplateKeyAndChannelAndLocale(key.value(), channel.code(), locale.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Template> findByKey(TemplateKey key) {
        return jpaRepository.findByTemplateKey(key.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Template> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
