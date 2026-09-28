package kz.company.notification.domain.preferences;

import kz.company.notification.domain.notification.RecipientId;

import java.util.Optional;

/** Хранилище агрегатов {@link RecipientPreferences}. */
public interface PreferencesRepository {

    RecipientPreferences save(RecipientPreferences preferences);

    Optional<RecipientPreferences> findByRecipientId(RecipientId recipientId);
}
