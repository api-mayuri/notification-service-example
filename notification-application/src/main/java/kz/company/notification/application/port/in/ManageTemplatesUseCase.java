package kz.company.notification.application.port.in;

import kz.company.notification.domain.template.TemplateKey;

import java.util.List;

/** Управление шаблонами и их версиями. */
public interface ManageTemplatesUseCase {

    TemplateView create(CreateTemplateCommand command);

    TemplateView addVersion(AddTemplateVersionCommand command);

    TemplateView activate(ActivateTemplateVersionCommand command);

    TemplateView updateSettings(UpdateTemplateSettingsCommand command);

    List<TemplateView> list();

    /** Все варианты (каналы и локали) шаблона с ключом. */
    List<TemplateView> findByKey(TemplateKey key);
}
