package kz.company.notification.application.service;

import kz.company.notification.application.ApplicationTestContext;
import kz.company.notification.application.port.in.ActivateTemplateVersionCommand;
import kz.company.notification.application.port.in.AddTemplateVersionCommand;
import kz.company.notification.application.port.in.CreateTemplateCommand;
import kz.company.notification.application.port.in.PreviewTemplateCommand;
import kz.company.notification.application.port.in.TemplatePreview;
import kz.company.notification.application.port.in.TemplateView;
import kz.company.notification.application.port.in.UpdateTemplateSettingsCommand;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.MissingTemplateVariablesException;
import kz.company.notification.domain.template.TemplateAlreadyExistsException;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateNotFoundException;
import kz.company.notification.domain.template.TemplateVersion;
import kz.company.notification.domain.template.TemplateVersionNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateManagementServiceTest {

    private static final TemplateKey WELCOME = new TemplateKey("welcome");
    private static final LocaleTag RU = LocaleTag.of("ru");

    private final ApplicationTestContext context = new ApplicationTestContext();

    private static TemplateContentDefinition content(String body) {
        return new TemplateContentDefinition("Привет, ${name}", body, null, Set.of("name"));
    }

    @Test
    void createAddActivateAndPreview() {
        TemplateView created = context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.EMAIL, RU,
                content("<p>v1 ${name}</p>"), null, true));
        TemplateView withDraft = context.templateService.addVersion(new AddTemplateVersionCommand(WELCOME,
                ChannelType.EMAIL, RU, content("<p>v2 ${name}</p>"), false));
        TemplatePreview draftPreview = context.templateService.preview(new PreviewTemplateCommand(WELCOME,
                ChannelType.EMAIL, RU, new TemplateVersion(2), Map.of("name", "Анна")));
        TemplatePreview activePreview = context.templateService.preview(new PreviewTemplateCommand(WELCOME,
                ChannelType.EMAIL, RU, null, Map.of("name", "Анна")));
        TemplateView activated = context.templateService.activate(new ActivateTemplateVersionCommand(WELCOME,
                ChannelType.EMAIL, RU, new TemplateVersion(2)));

        assertThat(created.activeVersion()).isEqualTo(1);
        assertThat(withDraft.activeVersion()).isEqualTo(1);
        assertThat(withDraft.versions()).hasSize(2);
        assertThat(draftPreview.htmlBody()).isEqualTo("<p>v2 Анна</p>");
        assertThat(draftPreview.subject()).isEqualTo("Привет, Анна");
        assertThat(activePreview.version()).isEqualTo(1);
        assertThat(activated.activeVersion()).isEqualTo(2);
        assertThat(activated.versions().getFirst().status()).isEqualTo("ARCHIVED");
        assertThat(context.renderer.evicted()).hasSize(1);
    }

    @Test
    void addVersionWithActivationEvictsCache() {
        context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.SMS, RU, content("v1"), null, false));

        TemplateView view = context.templateService.addVersion(new AddTemplateVersionCommand(WELCOME, ChannelType.SMS,
                RU, content("v2"), true));

        assertThat(view.activeVersion()).isEqualTo(2);
        assertThat(context.renderer.evicted()).hasSize(1);
    }

    @Test
    void duplicateTemplateIsRejected() {
        context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.EMAIL, RU, content("x"), null, false));

        assertThatThrownBy(() -> context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.EMAIL,
                RU, content("y"), null, false)))
                .isInstanceOf(TemplateAlreadyExistsException.class);
    }

    @Test
    void settingsListingAndLookup() {
        context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.SMS, RU, content("x"), null, false));
        context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.EMAIL, RU, content("x"), null, false));

        TemplateView updated = context.templateService.updateSettings(new UpdateTemplateSettingsCommand(WELCOME,
                ChannelType.EMAIL, RU, Duration.ofMinutes(5)));

        assertThat(updated.deduplicationWindow()).isEqualTo(Duration.ofMinutes(5));
        assertThat(context.templateService.list().stream().map(TemplateView::channel).toList())
                .containsExactly("EMAIL", "SMS");
        assertThat(context.templateService.findByKey(WELCOME)).hasSize(2);
        assertThatThrownBy(() -> context.templateService.findByKey(new TemplateKey("unknown")))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void previewValidatesVersionAndVariables() {
        context.templateService.create(new CreateTemplateCommand(WELCOME, ChannelType.SMS, RU, content("x"), null, false));

        assertThatThrownBy(() -> context.templateService.preview(new PreviewTemplateCommand(WELCOME, ChannelType.SMS,
                RU, new TemplateVersion(5), Map.of("name", "x"))))
                .isInstanceOf(TemplateVersionNotFoundException.class);
        assertThatThrownBy(() -> context.templateService.preview(new PreviewTemplateCommand(WELCOME, ChannelType.SMS,
                RU, null, null)))
                .isInstanceOf(MissingTemplateVariablesException.class);
        assertThatThrownBy(() -> context.templateService.activate(new ActivateTemplateVersionCommand(WELCOME,
                ChannelType.PUSH, RU, TemplateVersion.FIRST)))
                .isInstanceOf(TemplateNotFoundException.class);
    }
}
