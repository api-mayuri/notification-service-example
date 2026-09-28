package kz.company.notification.infrastructure.out.template;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.AlwaysValidCacheEntryValidity;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.Map;

/**
 * Резолвер шаблонов Thymeleaf из БД. Имя шаблона — {@link TemplatePart}: id шаблона, версия,
 * часть (тема, тело, текстовое тело) и режим (HTML для тела письма, TEXT для остального).
 */
public class DatabaseTemplateResolver extends AbstractTemplateResolver {

    private final TemplateSourceLoader sourceLoader;

    public DatabaseTemplateResolver(TemplateSourceLoader sourceLoader) {
        this.sourceLoader = sourceLoader;
        setName("database");
        setCheckExistence(false);
    }

    @Override
    protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
                                                        String template, Map<String, Object> templateResolutionAttributes) {
        return new StringTemplateResource(sourceLoader.load(TemplatePart.parse(template)));
    }

    @Override
    protected TemplateMode computeTemplateMode(IEngineConfiguration configuration, String ownerTemplate,
                                               String template, Map<String, Object> templateResolutionAttributes) {
        return TemplatePart.parse(template).mode();
    }

    @Override
    protected ICacheEntryValidity computeValidity(IEngineConfiguration configuration, String ownerTemplate,
                                                  String template, Map<String, Object> templateResolutionAttributes) {
        return AlwaysValidCacheEntryValidity.INSTANCE;
    }
}
