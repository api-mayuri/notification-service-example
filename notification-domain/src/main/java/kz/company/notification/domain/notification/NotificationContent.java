package kz.company.notification.domain.notification;

/**
 * Содержимое уведомления: либо ссылка на шаблон с переменными, либо готовый текст.
 */
public sealed interface NotificationContent permits TemplateContent, RawContent {
}
