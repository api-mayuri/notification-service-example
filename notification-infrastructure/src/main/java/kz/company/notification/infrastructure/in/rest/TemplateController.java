package kz.company.notification.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.company.notification.application.port.in.ActivateTemplateVersionCommand;
import kz.company.notification.application.port.in.AddTemplateVersionCommand;
import kz.company.notification.application.port.in.CreateTemplateCommand;
import kz.company.notification.application.port.in.ManageTemplatesUseCase;
import kz.company.notification.application.port.in.PreviewTemplateCommand;
import kz.company.notification.application.port.in.PreviewTemplateUseCase;
import kz.company.notification.application.port.in.TemplatePreview;
import kz.company.notification.application.port.in.TemplateView;
import kz.company.notification.application.port.in.UpdateTemplateSettingsCommand;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.template.LocaleTag;
import kz.company.notification.domain.template.TemplateContentDefinition;
import kz.company.notification.domain.template.TemplateKey;
import kz.company.notification.domain.template.TemplateVersion;
import kz.company.notification.infrastructure.in.rest.dto.TemplateRequests;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * REST API шаблонов. Шаблон определяется ключом, каналом и локалью; у него есть версии,
 * активна одна. Все операции — scope {@code templates:manage}.
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Шаблоны сообщений, версии и предпросмотр")
@SecurityRequirement(name = "bearer-jwt")
public class TemplateController {

    private final ManageTemplatesUseCase manageTemplates;
    private final PreviewTemplateUseCase previewTemplate;

    public TemplateController(ManageTemplatesUseCase manageTemplates, PreviewTemplateUseCase previewTemplate) {
        this.manageTemplates = manageTemplates;
        this.previewTemplate = previewTemplate;
    }

    @GetMapping
    @Operation(summary = "Все шаблоны")
    public List<TemplateView> list() {
        return manageTemplates.list();
    }

    @GetMapping("/{key}")
    @Operation(summary = "Варианты шаблона по каналам и локалям")
    @ApiResponse(responseCode = "404", description = "Шаблона нет")
    public List<TemplateView> get(@PathVariable String key) {
        return manageTemplates.findByKey(new TemplateKey(key));
    }

    @PostMapping
    @Operation(summary = "Создать шаблон с первой версией")
    @ApiResponse(responseCode = "201", description = "Создан")
    @ApiResponse(responseCode = "409", description = "Шаблон с таким ключом, каналом и локалью уже есть")
    @ApiResponse(responseCode = "422", description = "Неверный ключ, канал, локаль или содержимое")
    public ResponseEntity<TemplateView> create(@Valid @RequestBody TemplateRequests.Create request) {
        TemplateView view = manageTemplates.create(new CreateTemplateCommand(new TemplateKey(request.key()),
                ChannelType.of(request.channel()), LocaleTag.of(request.locale()),
                new TemplateContentDefinition(request.subject(), request.body(), request.textBody(),
                        request.requiredVariables()),
                request.deduplicationWindow(), request.activate()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/templates/{key}").buildAndExpand(view.key()).toUri()).body(view);
    }

    @PutMapping("/{key}")
    @Operation(summary = "Изменить настройки шаблона (окно дедупликации)")
    public TemplateView updateSettings(@PathVariable String key,
                                       @Valid @RequestBody TemplateRequests.UpdateSettings request) {
        return manageTemplates.updateSettings(new UpdateTemplateSettingsCommand(new TemplateKey(key),
                ChannelType.of(request.channel()), LocaleTag.of(request.locale()), request.deduplicationWindow()));
    }

    @PostMapping("/{key}/versions")
    @Operation(summary = "Добавить версию (DRAFT, по флагу activate — сразу активную)")
    @ApiResponse(responseCode = "201", description = "Версия добавлена")
    public ResponseEntity<TemplateView> addVersion(@PathVariable String key,
                                                   @Valid @RequestBody TemplateRequests.AddVersion request) {
        TemplateView view = manageTemplates.addVersion(new AddTemplateVersionCommand(new TemplateKey(key),
                ChannelType.of(request.channel()), LocaleTag.of(request.locale()),
                new TemplateContentDefinition(request.subject(), request.body(), request.textBody(),
                        request.requiredVariables()),
                request.activate()));
        return ResponseEntity.status(201).body(view);
    }

    @PostMapping("/{key}/activate")
    @Operation(summary = "Активировать версию; прежняя архивируется, кэш рендера сбрасывается")
    public TemplateView activate(@PathVariable String key, @Valid @RequestBody TemplateRequests.Activate request) {
        return manageTemplates.activate(new ActivateTemplateVersionCommand(new TemplateKey(key),
                ChannelType.of(request.channel()), LocaleTag.of(request.locale()), new TemplateVersion(request.version())));
    }

    @PostMapping("/{key}/preview")
    @Operation(summary = "Рендер с тестовыми переменными без отправки")
    @ApiResponse(responseCode = "422", description = "Не хватает обязательных переменных или шаблон не рендерится")
    public TemplatePreview preview(@PathVariable String key, @Valid @RequestBody TemplateRequests.Preview request) {
        return previewTemplate.preview(new PreviewTemplateCommand(new TemplateKey(key), ChannelType.of(request.channel()),
                LocaleTag.of(request.locale()), request.version() == null ? null : new TemplateVersion(request.version()),
                request.variables()));
    }
}
