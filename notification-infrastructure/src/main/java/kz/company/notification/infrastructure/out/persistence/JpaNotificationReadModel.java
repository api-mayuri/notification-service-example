package kz.company.notification.infrastructure.out.persistence;

import kz.company.notification.application.port.in.NotificationSearchCriteria;
import kz.company.notification.application.port.in.NotificationSummaryView;
import kz.company.notification.application.port.in.PageQuery;
import kz.company.notification.application.port.in.PageResult;
import kz.company.notification.application.port.out.NotificationReadModel;
import kz.company.notification.infrastructure.out.persistence.entity.NotificationJpaEntity;
import kz.company.notification.infrastructure.out.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Поиск уведомлений через JPA Specification. */
@Component
public class JpaNotificationReadModel implements NotificationReadModel {

    private final NotificationJpaRepository jpaRepository;

    public JpaNotificationReadModel(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PageResult<NotificationSummaryView> search(NotificationSearchCriteria criteria, PageQuery page) {
        Page<NotificationJpaEntity> result = jpaRepository.findAll(NotificationSpecifications.matching(criteria),
                PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(result.getContent().stream().map(JpaNotificationReadModel::toSummary).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    private static NotificationSummaryView toSummary(NotificationJpaEntity entity) {
        return new NotificationSummaryView(entity.getId(), entity.getRecipientId(), entity.getStatus(),
                entity.getCategory(), entity.getPriority(),
                Arrays.stream(entity.getChannels().split(",")).filter(code -> !code.isBlank()).toList(),
                entity.getTemplateKey(), entity.getCreatedAt());
    }
}
