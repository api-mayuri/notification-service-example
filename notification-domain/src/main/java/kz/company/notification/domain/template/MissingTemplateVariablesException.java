package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;

import java.util.Set;

/** Не переданы обязательные переменные шаблона. Проверяется при приёме запроса, а не при доставке. */
public class MissingTemplateVariablesException extends DomainValidationException {

    private final Set<String> missingVariables;

    public MissingTemplateVariablesException(TemplateKey key, Set<String> missingVariables) {
        super("MISSING_TEMPLATE_VARIABLES", "Template " + key + " requires variables " + missingVariables);
        this.missingVariables = Set.copyOf(missingVariables);
    }

    public Set<String> missingVariables() {
        return missingVariables;
    }
}
