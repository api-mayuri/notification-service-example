package kz.company.notification.application.port.in;

/** Отправлено, доставлено, ошибки — по каналам и периодам. */
public interface GetStatisticsQuery {

    StatisticsView statistics(StatisticsCriteria criteria);
}
