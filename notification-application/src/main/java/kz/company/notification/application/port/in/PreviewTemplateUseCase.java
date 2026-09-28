package kz.company.notification.application.port.in;

/** Рендер шаблона с тестовыми переменными без отправки. */
public interface PreviewTemplateUseCase {

    TemplatePreview preview(PreviewTemplateCommand command);
}
