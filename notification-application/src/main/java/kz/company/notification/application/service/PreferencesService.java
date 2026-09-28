package kz.company.notification.application.service;

import kz.company.notification.application.port.in.ManagePreferencesUseCase;
import kz.company.notification.application.port.in.PreferencesView;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesChange;
import kz.company.notification.domain.preferences.PreferencesRepository;
import kz.company.notification.domain.preferences.QuietHours;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.domain.template.LocaleTag;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Предпочтения получателя. */
public class PreferencesService implements ManagePreferencesUseCase {

    private final PreferencesRepository preferencesRepository;
    private final Clock clock;

    public PreferencesService(PreferencesRepository preferencesRepository, Clock clock) {
        this.preferencesRepository = preferencesRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferencesView get(RecipientId recipientId) {
        return toView(load(recipientId));
    }

    @Override
    @Transactional
    public PreferencesView change(RecipientId recipientId, PreferencesChange change) {
        RecipientPreferences preferences = load(recipientId);
        preferences.apply(change, clock.instant());
        return toView(preferencesRepository.save(preferences));
    }

    private RecipientPreferences load(RecipientId recipientId) {
        return preferencesRepository.findByRecipientId(recipientId)
                .orElseGet(() -> RecipientPreferences.defaults(recipientId));
    }

    private static PreferencesView toView(RecipientPreferences preferences) {
        return new PreferencesView(
                preferences.recipientId().value(),
                preferences.unsubscribedChannels().stream().map(ChannelType::code)
                        .collect(Collectors.toCollection(TreeSet::new)),
                preferences.unsubscribedCategories().stream().map(Category::name)
                        .collect(Collectors.toCollection(TreeSet::new)),
                preferences.quietHours().map(QuietHours::start).orElse(null),
                preferences.quietHours().map(QuietHours::end).orElse(null),
                preferences.timeZone().getId(),
                preferences.locale().map(LocaleTag::value).orElse(null),
                preferences.updatedAt().orElse(null));
    }
}
