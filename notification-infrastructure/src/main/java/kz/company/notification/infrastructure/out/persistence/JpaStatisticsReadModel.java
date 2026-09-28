package kz.company.notification.infrastructure.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.in.StatisticsRow;
import kz.company.notification.application.port.out.StatisticsReadModel;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

/** Счётчики доставок: группировка по периоду (UTC), каналу и статусу средствами PostgreSQL. */
@Component
public class JpaStatisticsReadModel implements StatisticsReadModel {

    private final EntityManager entityManager;

    public JpaStatisticsReadModel(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StatisticsRow> countDeliveries(StatisticsCriteria criteria) {
        String unit = criteria.granularity().name().toLowerCase(Locale.ROOT);
        StringBuilder sql = new StringBuilder("""
                select date_trunc('%s', d.created_at, 'UTC') as period_start, d.channel, d.status, count(*) as total
                from delivery d
                join notification n on n.id = d.notification_id
                where n.client_id = :clientId and d.created_at >= :from and d.created_at < :to
                """.formatted(unit));
        if (criteria.channel() != null) {
            sql.append(" and d.channel = :channel");
        }
        sql.append(" group by 1, 2, 3 order by 1, 2, 3");
        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("clientId", criteria.clientId().value())
                .setParameter("from", criteria.from())
                .setParameter("to", criteria.to());
        if (criteria.channel() != null) {
            query.setParameter("channel", criteria.channel().code());
        }
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new StatisticsRow(toInstant(row[0]), (String) row[1], (String) row[2],
                        ((Number) row[3]).longValue()))
                .toList();
    }

    private static Instant toInstant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("Unexpected period type " + value.getClass());
        };
    }
}
