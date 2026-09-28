package kz.company.notification.infrastructure.out.persistence.repository;

import kz.company.notification.infrastructure.out.persistence.entity.TemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateJpaRepository extends JpaRepository<TemplateJpaEntity, UUID> {

    Optional<TemplateJpaEntity> findByTemplateKeyAndChannelAndLocale(String templateKey, String channel, String locale);

    List<TemplateJpaEntity> findByTemplateKey(String templateKey);
}
