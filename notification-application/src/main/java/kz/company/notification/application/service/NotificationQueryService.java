package kz.company.notification.application.service;

import kz.company.notification.application.port.in.GetNotificationQuery;
import kz.company.notification.application.port.in.NotificationSearchCriteria;
import kz.company.notification.application.port.in.NotificationSummaryView;
import kz.company.notification.application.port.in.NotificationView;
import kz.company.notification.application.port.in.PageQuery;
import kz.company.notification.application.port.in.PageResult;
import kz.company.notification.application.port.in.SearchNotificationsQuery;
import kz.company.notification.application.port.out.NotificationReadModel;
import kz.company.notification.domain.delivery.DeliveryRepository;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.Notification;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.NotificationNotFoundException;
import kz.company.notification.domain.notification.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

/** Чтение уведомлений. Отправитель видит только свои уведомления: чужие — как несуществующие. */
public class NotificationQueryService implements GetNotificationQuery, SearchNotificationsQuery {

    private final NotificationRepository notificationRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationReadModel readModel;

    public NotificationQueryService(NotificationRepository notificationRepository,
                                    DeliveryRepository deliveryRepository,
                                    NotificationReadModel readModel) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.readModel = readModel;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationView get(ClientId clientId, NotificationId notificationId) {
        Notification notification = loadOwned(notificationRepository, clientId, notificationId);
        return NotificationViewAssembler.toView(notification, deliveryRepository.findByNotificationId(notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationSummaryView> search(NotificationSearchCriteria criteria, PageQuery page) {
        return readModel.search(criteria, page);
    }

    static Notification loadOwned(NotificationRepository repository, ClientId clientId, NotificationId id) {
        return repository.findById(id)
                .filter(notification -> notification.clientId().equals(clientId))
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
