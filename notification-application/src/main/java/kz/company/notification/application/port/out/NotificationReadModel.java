package kz.company.notification.application.port.out;

import kz.company.notification.application.port.in.NotificationSearchCriteria;
import kz.company.notification.application.port.in.NotificationSummaryView;
import kz.company.notification.application.port.in.PageQuery;
import kz.company.notification.application.port.in.PageResult;

/** Запросы чтения для поиска уведомлений (Specification на стороне хранилища). */
public interface NotificationReadModel {

    PageResult<NotificationSummaryView> search(NotificationSearchCriteria criteria, PageQuery page);
}
