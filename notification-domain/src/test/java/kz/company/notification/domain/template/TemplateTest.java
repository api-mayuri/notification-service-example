package kz.company.notification.domain.template;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.delivery.ChannelType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static kz.company.notification.domain.TestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateTest {

    private static TemplateContentDefinition content(String body) {
        return new TemplateContentDefinition("Заказ [(${orderId})]", body, null, Set.of("orderId"));
    }

    private static Template newTemplate() {
        return Template.create(new TemplateId(UUID.randomUUID()), new TemplateKey("order-shipped"), ChannelType.EMAIL,
                LocaleTag.of("ru"), content("<p th:text=\"${orderId}\"></p>"), null, NOW);
    }

    @Test
    void newTemplateHasDraftFirstVersionAndNoActiveVersion() {
        Template template = newTemplate();

        assertThat(template.revisions()).hasSize(1);
        assertThat(template.latestRevision().status()).isEqualTo(TemplateStatus.DRAFT);
        assertThat(template.activeRevision()).isEmpty();
    }

    @Test
    void activatingNewVersionArchivesPrevious() {
        Template template = newTemplate();
        template.activate(TemplateVersion.FIRST, NOW);
        TemplateVersion second = template.addRevision(content("<p>v2</p>"), NOW);

        template.activate(second, NOW);
        template.activate(second, NOW);

        assertThat(second.value()).isEqualTo(2);
        assertThat(template.activeRevision().orElseThrow().version()).isEqualTo(second);
        assertThat(template.revision(TemplateVersion.FIRST).orElseThrow().status()).isEqualTo(TemplateStatus.ARCHIVED);
    }

    @Test
    void activatingUnknownVersionFails() {
        Template template = newTemplate();

        assertThatThrownBy(() -> template.activate(new TemplateVersion(9), NOW))
                .isInstanceOf(TemplateVersionNotFoundException.class);
    }

    @Test
    void missingVariablesAreReported() {
        Template template = newTemplate();
        TemplateRevision revision = template.latestRevision();

        assertThat(revision.missingVariables(Map.of())).containsExactly("orderId");
        assertThatThrownBy(() -> revision.requireVariables(template.key(), Map.of("other", 1)))
                .isInstanceOf(MissingTemplateVariablesException.class)
                .hasMessageContaining("orderId");
        revision.requireVariables(template.key(), Map.of("orderId", "ORD-1"));
    }

    @Test
    void deduplicationWindowIsLimited() {
        Template template = newTemplate();

        template.changeDeduplicationWindow(Duration.ofMinutes(10), NOW);

        assertThat(template.deduplicationWindow()).contains(Duration.ofMinutes(10));
        assertThatThrownBy(() -> template.changeDeduplicationWindow(Duration.ofDays(2), NOW))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> template.changeDeduplicationWindow(Duration.ZERO, NOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void snapshotRoundTrip() {
        Template template = newTemplate();
        template.addRevision(content("<p>v2</p>"), NOW);

        Template restored = Template.restore(template.snapshot());

        assertThat(restored.snapshot()).isEqualTo(template.snapshot());
    }

    @Test
    void contentValidation() {
        assertThatThrownBy(() -> new TemplateContentDefinition(null, " ", null, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TemplateContentDefinition(null, "body", null, Set.of("1bad")))
                .isInstanceOf(DomainValidationException.class);
        assertThat(new TemplateContentDefinition(null, "body", null, null).requiredVariables()).isEmpty();
    }

    @Test
    void keyAndVersionValidation() {
        assertThatThrownBy(() -> new TemplateKey("Order_Shipped")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TemplateVersion(0)).isInstanceOf(DomainValidationException.class);
        assertThat(TemplateVersion.FIRST.next().compareTo(TemplateVersion.FIRST)).isEqualTo(1);
    }
}
