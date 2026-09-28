package kz.company.notification.infrastructure.out.persistence.repository;

import kz.company.notification.infrastructure.out.persistence.entity.RecipientPreferencesJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientPreferencesJpaRepository extends JpaRepository<RecipientPreferencesJpaEntity, String> {
}
