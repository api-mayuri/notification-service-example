package kz.company.notification.application.service;

import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.ActiveTemplateNotFoundException;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateRepository;

import java.util.List;
import java.util.Optional;

/**
 * Поиск активной версии шаблона с fallback по локали: {@code ru-KZ → ru → локаль по умолчанию}.
 */
public final class TemplateLookup {

    private final TemplateRepository templateRepository;
    private final LocaleTag defaultLocale;

    public TemplateLookup(TemplateRepository templateRepository, LocaleTag defaultLocale) {
        this.templateRepository = templateRepository;
        this.defaultLocale = defaultLocale;
    }

    public ResolvedTemplate resolveActive(TemplateKey key, ChannelType channel, LocaleTag locale) {
        List<LocaleTag> chain = locale.fallbackChain(defaultLocale);
        for (LocaleTag candidate : chain) {
            Optional<ResolvedTemplate> resolved = templateRepository.find(key, channel, candidate)
                    .flatMap(template -> template.activeRevision().map(revision -> new ResolvedTemplate(template, revision)));
            if (resolved.isPresent()) {
                return resolved.get();
            }
        }
        throw new ActiveTemplateNotFoundException(key, channel, chain);
    }
}
