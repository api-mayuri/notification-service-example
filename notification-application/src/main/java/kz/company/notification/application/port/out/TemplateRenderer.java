package kz.company.notification.application.port.out;

import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.Template;

import java.util.Map;

/**
 * Рендер шаблона с переменными. Переменные — это данные пользователя: реализация обязана
 * экранировать их в HTML и не выполнять выражения из их значений.
 */
public interface TemplateRenderer {

    /** Отрендерить версию шаблона; в результате сохраняются ключ, версия и локаль шаблона. */
    RenderedMessage render(ResolvedTemplate template, Map<String, Object> variables);

    /** Сбросить кэш шаблона (после активации новой версии). */
    void evict(Template template);
}
