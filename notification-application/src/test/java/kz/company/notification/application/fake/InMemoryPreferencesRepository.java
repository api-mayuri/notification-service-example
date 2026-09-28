package kz.company.notification.application.fake;

import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesRepository;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.domain.preferences.RecipientPreferencesSnapshot;

import java.util.Optional;

public final class InMemoryPreferencesRepository implements PreferencesRepository {

    private final VersionedStore<RecipientId, RecipientPreferencesSnapshot> store = new VersionedStore<>();

    @Override
    public RecipientPreferences save(RecipientPreferences preferences) {
        RecipientPreferencesSnapshot s = preferences.snapshot();
        store.put(s.recipientId(), s.version(), version -> new RecipientPreferencesSnapshot(s.recipientId(),
                s.unsubscribedChannels(), s.unsubscribedCategories(), s.quietHours(), s.timeZone(), s.locale(),
                s.updatedAt(), version));
        return findByRecipientId(s.recipientId()).orElseThrow();
    }

    @Override
    public Optional<RecipientPreferences> findByRecipientId(RecipientId recipientId) {
        return store.get(recipientId).map(RecipientPreferences::restore);
    }
}
