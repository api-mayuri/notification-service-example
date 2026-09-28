package kz.company.notification.infrastructure.out.template;

import kz.company.notification.application.port.out.TemplateRenderer;
import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import org.springframework.stereotype.Component;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;
import java.util.UUID;

/**
 * Рендер шаблонов Thymeleaf: HTML-режим для тела письма, TEXT-режим для темы и остальных каналов.
 *
 * <p>Безопасность: переменные — данные пользователя, они попадают в контекст как значения и никогда
 * не интерпретируются как выражения. В HTML-режиме {@code th:text} экранирует вывод. Выражения
 * пишут только администраторы шаблонов (scope {@code templates:manage}); Thymeleaf 3.1 дополнительно
 * запрещает в выражениях доступ к статическим классам и объектам запроса.
 */
@Component
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private static final int CACHE_SIZE = 1000;

    private final SpringTemplateEngine engine;

    public ThymeleafTemplateRenderer(TemplateSourceLoader sourceLoader) {
        DatabaseTemplateResolver resolver = new DatabaseTemplateResolver(sourceLoader);
        StandardCacheManager cacheManager = new StandardCacheManager();
        cacheManager.setTemplateCacheMaxSize(CACHE_SIZE);
        this.engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setCacheManager(cacheManager);
        engine.setEnableSpringELCompiler(false);
    }

    @Override
    public RenderedMessage render(ResolvedTemplate resolved, Map<String, Object> variables) {
        Template template = resolved.template();
        TemplateContentDefinition content = resolved.revision().content();
        UUID templateId = template.id().value();
        int version = resolved.revision().version().value();
        Context context = new Context(template.locale().toLocale(), variables);
        try {
            String subject = content.subject() == null ? null
                    : process(new TemplatePart(templateId, version, TemplatePart.Part.SUBJECT, TemplateMode.TEXT), context).strip();
            String textBody;
            String htmlBody;
            if (template.channel().equals(ChannelType.EMAIL)) {
                htmlBody = process(new TemplatePart(templateId, version, TemplatePart.Part.BODY, TemplateMode.HTML), context);
                textBody = content.textBody() == null ? null
                        : process(new TemplatePart(templateId, version, TemplatePart.Part.TEXT_BODY, TemplateMode.TEXT), context);
            } else {
                htmlBody = null;
                textBody = process(new TemplatePart(templateId, version, TemplatePart.Part.BODY, TemplateMode.TEXT), context);
            }
            return new RenderedMessage(subject, textBody, htmlBody, template.key().value(), version,
                    template.locale().value());
        } catch (TemplateEngineException exception) {
            throw new DomainValidationException("TEMPLATE_RENDERING_FAILED",
                    "Template " + template.key() + " v" + version + " cannot be rendered: " + exception.getMessage());
        }
    }

    @Override
    public void evict(Template template) {
        engine.clearTemplateCache();
    }

    private String process(TemplatePart part, Context context) {
        return engine.process(part.templateName(), context);
    }
}
