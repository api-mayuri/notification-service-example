package kz.company.notification.application.service;

import kz.company.notification.application.port.in.ActivateTemplateVersionCommand;
import kz.company.notification.application.port.in.AddTemplateVersionCommand;
import kz.company.notification.application.port.in.CreateTemplateCommand;
import kz.company.notification.application.port.in.ManageTemplatesUseCase;
import kz.company.notification.application.port.in.PreviewTemplateCommand;
import kz.company.notification.application.port.in.PreviewTemplateUseCase;
import kz.company.notification.application.port.in.TemplatePreview;
import kz.company.notification.application.port.in.TemplateView;
import kz.company.notification.application.port.in.UpdateTemplateSettingsCommand;
import kz.company.notification.application.port.out.IdGenerator;
import kz.company.notification.application.port.out.TemplateRenderer;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateAlreadyExistsException;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateNotFoundException;
import kz.company.notification.domain.template.TemplateRepository;
import kz.company.notification.domain.template.TemplateRevision;
import kz.company.notification.domain.template.TemplateVersion;
import kz.company.notification.domain.template.TemplateVersionNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Управление шаблонами и предпросмотр. */
public class TemplateManagementService implements ManageTemplatesUseCase, PreviewTemplateUseCase {

    private static final Comparator<Template> ORDER = Comparator
            .comparing((Template template) -> template.key().value())
            .thenComparing(Template::channel)
            .thenComparing(template -> template.locale().value());

    private final TemplateRepository templateRepository;
    private final TemplateRenderer templateRenderer;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public TemplateManagementService(TemplateRepository templateRepository, TemplateRenderer templateRenderer,
                                     IdGenerator idGenerator, Clock clock) {
        this.templateRepository = templateRepository;
        this.templateRenderer = templateRenderer;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TemplateView create(CreateTemplateCommand command) {
        if (templateRepository.find(command.key(), command.channel(), command.locale()).isPresent()) {
            throw new TemplateAlreadyExistsException(command.key(), command.channel(), command.locale());
        }
        Instant now = clock.instant();
        Template template = Template.create(new TemplateId(idGenerator.newId()), command.key(), command.channel(),
                command.locale(), command.content(), command.deduplicationWindow(), now);
        if (command.activate()) {
            template.activate(TemplateVersion.FIRST, now);
        }
        return TemplateViewAssembler.toView(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateView addVersion(AddTemplateVersionCommand command) {
        Instant now = clock.instant();
        Template template = load(command.key(), command.channel(), command.locale());
        TemplateVersion version = template.addRevision(command.content(), now);
        if (command.activate()) {
            template.activate(version, now);
        }
        Template saved = templateRepository.save(template);
        if (command.activate()) {
            templateRenderer.evict(saved);
        }
        return TemplateViewAssembler.toView(saved);
    }

    @Override
    @Transactional
    public TemplateView activate(ActivateTemplateVersionCommand command) {
        Template template = load(command.key(), command.channel(), command.locale());
        template.activate(command.version(), clock.instant());
        Template saved = templateRepository.save(template);
        templateRenderer.evict(saved);
        return TemplateViewAssembler.toView(saved);
    }

    @Override
    @Transactional
    public TemplateView updateSettings(UpdateTemplateSettingsCommand command) {
        Template template = load(command.key(), command.channel(), command.locale());
        template.changeDeduplicationWindow(command.deduplicationWindow(), clock.instant());
        return TemplateViewAssembler.toView(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateView> list() {
        return templateRepository.findAll().stream().sorted(ORDER).map(TemplateViewAssembler::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateView> findByKey(TemplateKey key) {
        List<Template> templates = templateRepository.findByKey(key);
        if (templates.isEmpty()) {
            throw new TemplateNotFoundException(key);
        }
        return templates.stream().sorted(ORDER).map(TemplateViewAssembler::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplatePreview preview(PreviewTemplateCommand command) {
        Template template = load(command.key(), command.channel(), command.locale());
        TemplateRevision revision = command.version() != null
                ? template.revision(command.version()).orElseThrow(() -> new TemplateVersionNotFoundException(
                        command.key(), command.channel(), command.locale(), command.version()))
                : template.activeRevision().orElseGet(template::latestRevision);
        revision.requireVariables(command.key(), command.variables());
        RenderedMessage message = templateRenderer.render(new ResolvedTemplate(template, revision), command.variables());
        return new TemplatePreview(message.subject(), message.textBody(), message.htmlBody(),
                revision.version().value(), template.locale().value());
    }

    private Template load(TemplateKey key, ChannelType channel, LocaleTag locale) {
        return templateRepository.find(key, channel, locale)
                .orElseThrow(() -> new TemplateNotFoundException(key, channel, locale));
    }
}
