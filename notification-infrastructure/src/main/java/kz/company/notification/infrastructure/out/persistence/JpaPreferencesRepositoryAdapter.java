package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.domain.notification.RecipientId;
import kz.company.notification.domain.preferences.PreferencesRepository;
import kz.company.notification.domain.preferences.RecipientPreferences;
import kz.company.notification.infrastructure.out.persistence.mapper.PreferencesMapper;
import kz.company.notification.infrastructure.out.persistence.repository.RecipientPreferencesJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Хранилище предпочтений получателей на JPA. */
@Repository
public class JpaPreferencesRepositoryAdapter implements PreferencesRepository {

    private final RecipientPreferencesJpaRepository jpaRepository;
    private final PreferencesMapper mapper;

    public JpaPreferencesRepositoryAdapter(RecipientPreferencesJpaRepository jpaRepository, PreferencesMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RecipientPreferences save(RecipientPreferences preferences) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(preferences)));
    }

    @Override
    public Optional<RecipientPreferences> findByRecipientId(RecipientId recipientId) {
        return jpaRepository.findById(recipientId.value()).map(mapper::toDomain);
    }
}
