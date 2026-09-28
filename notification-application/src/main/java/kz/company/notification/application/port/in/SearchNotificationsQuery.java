package kz.company.notification.application.port.in;

/** Поиск уведомлений с пагинацией, сортировка — по времени создания, новые первыми. */
public interface SearchNotificationsQuery {

    PageResult<NotificationSummaryView> search(NotificationSearchCriteria criteria, PageQuery page);
}
