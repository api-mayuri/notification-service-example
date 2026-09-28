package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Содержимое версии шаблона.
 *
 * @param subject           тема (для email; для остальных каналов необязательна)
 * @param body              основное тело: HTML для email, текст для остальных каналов
 * @param textBody          текстовая версия письма; если нет — получается из HTML
 * @param requiredVariables переменные, без которых шаблон нельзя отрендерить
 */
public record TemplateContentDefinition(String subject, String body, String textBody, Set<String> requiredVariables) {

    private static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");
    public static final int MAX_BODY_LENGTH = 200_000;

    public TemplateContentDefinition {
        Guard.maxLength(subject, 998, "subject");
        Guard.notBlank(body, "body");
        Guard.maxLength(body, MAX_BODY_LENGTH, "body");
        Guard.maxLength(textBody, MAX_BODY_LENGTH, "textBody");
        Set<String> variables = new TreeSet<>();
        if (requiredVariables != null) {
            for (String name : requiredVariables) {
                if (name == null || !VARIABLE_NAME.matcher(name).matches()) {
                    throw DomainValidationException.invalidValue("requiredVariables", "invalid variable name: " + name);
                }
                variables.add(name);
            }
        }
        requiredVariables = Collections.unmodifiableSet(variables);
    }
}
