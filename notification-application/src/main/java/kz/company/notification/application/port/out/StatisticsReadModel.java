package kz.company.notification.application.port.out;

import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.in.StatisticsRow;

import java.util.List;

/** Агрегированные счётчики доставок по периодам, каналам и статусам. */
public interface StatisticsReadModel {

    List<StatisticsRow> countDeliveries(StatisticsCriteria criteria);
}
