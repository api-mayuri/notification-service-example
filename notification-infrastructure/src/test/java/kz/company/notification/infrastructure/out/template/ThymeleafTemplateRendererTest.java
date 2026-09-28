package kz.company.notification.infrastructure.out.template;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.delivery.RenderedMessage;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.ResolvedTemplate;
import kz.company.notification.domain.template.Template;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateId;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThymeleafTemplateRendererTest {

    private final TemplateSourceLoader loader = mock(TemplateSourceLoader.class);
    private final ThymeleafTemplateRenderer renderer = new ThymeleafTemplateRenderer(loader);

    private static ResolvedTemplate template(ChannelType channel, TemplateContentDefinition content) {
        Template template = Template.create(new TemplateId(UUID.randomUUID()), new TemplateKey("order-shipped"), channel,
                LocaleTag.of("ru"), content, null, Instant.EPOCH);
        template.activate(TemplateVersion.FIRST, Instant.EPOCH);
        return new ResolvedTemplate(template, template.activeRevision().orElseThrow());
    }

    private void loaderReturnsContentOf(TemplateContentDefinition content) {
        when(loader.load(any())).thenAnswer(invocation -> {
            TemplatePart part = invocation.getArgument(0);
            return switch (part.part()) {
                case SUBJECT -> content.subject();
                case BODY -> content.body();
                case TEXT_BODY -> content.textBody();
            };
        });
    }

    @Test
    void emailBodyIsHtmlWithEscapedVariables() {
        TemplateContentDefinition content = new TemplateContentDefinition("Заказ [(${orderId})]",
                "<p>Заказ <b th:text=\"${orderId}\">X</b></p>", "Заказ [(${orderId})]", Set.of("orderId"));
        loaderReturnsContentOf(content);

        RenderedMessage message = renderer.render(template(ChannelType.EMAIL, content),
                Map.of("orderId", "<script>alert(1)</script>"));

        assertThat(message.subject()).isEqualTo("Заказ <script>alert(1)</script>");
        assertThat(message.htmlBody()).contains("&lt;script&gt;alert(1)&lt;/script&gt;").doesNotContain("<script>");
        assertThat(message.textBody()).isEqualTo("Заказ <script>alert(1)</script>");
        assertThat(message.templateVersion()).isEqualTo(1);
        assertThat(message.templateLocale()).isEqualTo("ru");
    }

    @Test
    void variableValuesAreNotEvaluatedAsExpressions() {
        TemplateContentDefinition content = new TemplateContentDefinition(null, "Привет, [(${name})]!", null,
                Set.of("name"));
        loaderReturnsContentOf(content);

        RenderedMessage message = renderer.render(template(ChannelType.SMS, content),
                Map.of("name", "${T(java.lang.Runtime).getRuntime()}"));

        assertThat(message.textBody()).isEqualTo("Привет, ${T(java.lang.Runtime).getRuntime()}!");
        assertThat(message.htmlBody()).isNull();
        assertThat(message.subject()).isNull();
    }

    @Test
    void parsedTemplatesAreCachedUntilEviction() {
        TemplateContentDefinition content = new TemplateContentDefinition(null, "Код [(${code})]", null, Set.of("code"));
        loaderReturnsContentOf(content);
        ResolvedTemplate resolved = template(ChannelType.SMS, content);

        renderer.render(resolved, Map.of("code", "1"));
        renderer.render(resolved, Map.of("code", "2"));
        renderer.evict(resolved.template());
        renderer.render(resolved, Map.of("code", "3"));

        verify(loader, times(2)).load(any());
    }

    @Test
    void brokenTemplateIsReportedAsValidationError() {
        TemplateContentDefinition content = new TemplateContentDefinition(null, "[(${unclosed)]", null, Set.of());
        loaderReturnsContentOf(content);

        assertThatThrownBy(() -> renderer.render(template(ChannelType.SMS, content), Map.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("cannot be rendered");
    }
}
