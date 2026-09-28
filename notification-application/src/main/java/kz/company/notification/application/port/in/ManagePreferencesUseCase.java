package kz.company.notification.application.port.in;

import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesChange;

/** Чтение и изменение предпочтений получателя. */
public interface ManagePreferencesUseCase {

    /** Текущие предпочтения или значения по умолчанию, если получатель ничего не настраивал. */
    PreferencesView get(RecipientId recipientId);

    /** Применить изменение: полную замену (PUT) или частичное (PATCH). */
    PreferencesView change(RecipientId recipientId, PreferencesChange change);
}
