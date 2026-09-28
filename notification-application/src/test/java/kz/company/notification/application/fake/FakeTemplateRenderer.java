package kz.company.notification.application.fake;

import kz.company.notification.application.port.out.TemplateRenderer;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Подставляет {@code ${name}} без Thymeleaf. */
public final class FakeTemplateRenderer implements TemplateRenderer {

    private final List<Template> evicted = new ArrayList<>();

    @Override
    public RenderedMessage render(ResolvedTemplate template, Map<String, Object> variables) {
        String subject = substitute(template.revision().content().subject(), variables);
        String body = substitute(template.revision().content().body(), variables);
        boolean html = template.template().channel().equals(ChannelType.EMAIL);
        return new RenderedMessage(subject, html ? substitute(template.revision().content().textBody(), variables) : body,
                html ? body : null, template.template().key().value(), template.revision().version().value(),
                template.template().locale().value());
    }

    @Override
    public void evict(Template template) {
        evicted.add(template);
    }

    public List<Template> evicted() {
        return evicted;
    }

    private static String substitute(String text, Map<String, Object> variables) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
