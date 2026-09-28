package kz.company.notification.application.service;

import kz.company.notification.application.port.in.ReleaseExpiredIdempotencyKeysUseCase;
import kz.company.notification.application.settings.DeliverySettings;
import kz.company.notification.domain.notification.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Освобождение ключей идемпотентности старше срока хранения. */
public class IdempotencyMaintenanceService implements ReleaseExpiredIdempotencyKeysUseCase {

    private final NotificationRepository notificationRepository;
    private final DeliverySettings settings;
    private final Clock clock;

    public IdempotencyMaintenanceService(NotificationRepository notificationRepository, DeliverySettings settings,
                                         Clock clock) {
        this.notificationRepository = notificationRepository;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int releaseExpired() {
        return notificationRepository.releaseIdempotencyKeysOlderThan(clock.instant(), settings.idempotencyKeyRetention());
    }
}
