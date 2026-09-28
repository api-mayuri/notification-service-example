package kz.company.notification.application.service;

import kz.company.notification.application.port.in.GetStatisticsQuery;
import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.in.StatisticsRow;
import kz.company.notification.application.port.in.StatisticsView;
import kz.company.notification.application.port.out.StatisticsReadModel;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Статистика доставок по каналам, статусам и периодам. */
public class StatisticsService implements GetStatisticsQuery {

    private final StatisticsReadModel readModel;

    public StatisticsService(StatisticsReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsView statistics(StatisticsCriteria criteria) {
        List<StatisticsRow> rows = readModel.countDeliveries(criteria);
        Map<String, Map<String, Long>> totals = new TreeMap<>();
        for (StatisticsRow row : rows) {
            totals.computeIfAbsent(row.channel(), channel -> new TreeMap<>())
                    .merge(row.status(), row.count(), Long::sum);
        }
        return new StatisticsView(criteria.from(), criteria.to(), criteria.granularity().name(), totals, rows);
    }
}
