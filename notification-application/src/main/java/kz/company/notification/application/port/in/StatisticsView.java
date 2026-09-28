package kz.company.notification.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Статистика доставок.
 *
 * @param totals итог по каналу: статус → количество
 * @param rows   разбивка по периодам
 */
public record StatisticsView(Instant from, Instant to, String granularity,
                             Map<String, Map<String, Long>> totals, List<StatisticsRow> rows) {
}
