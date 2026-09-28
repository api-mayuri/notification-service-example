package kz.company.notification.infrastructure.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.company.notification.application.port.in.GetStatisticsQuery;
import kz.company.notification.application.port.in.StatisticsCriteria;
import kz.company.notification.application.port.in.StatisticsView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Статистика доставок отправителя. Scope {@code notifications:read}. */
@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Statistics", description = "Отправлено, доставлено, ошибки по каналам и периодам")
@SecurityRequirement(name = "bearer-jwt")
public class StatisticsController {

    private final GetStatisticsQuery statisticsQuery;
    private final Clock clock;

    public StatisticsController(GetStatisticsQuery statisticsQuery, Clock clock) {
        this.statisticsQuery = statisticsQuery;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Счётчики доставок",
            description = "По умолчанию — последние 7 дней с шагом DAY. Период не длиннее 92 дней.")
    public StatisticsView statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String granularity,
            Authentication authentication) {
        Instant end = to != null ? to : clock.instant();
        Instant start = from != null ? from : end.minus(Duration.ofDays(7));
        return statisticsQuery.statistics(new StatisticsCriteria(AuthenticatedClient.clientId(authentication), start, end,
                RequestValues.channel(channel),
                RequestValues.enumValue(StatisticsCriteria.Granularity.class, granularity, "granularity")));
    }
}
