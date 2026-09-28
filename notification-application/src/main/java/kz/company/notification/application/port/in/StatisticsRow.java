package kz.company.notification.application.port.in;

import java.time.Instant;

/** Число доставок с данным статусом по каналу за период, начинающийся в {@code periodStart}. */
public record StatisticsRow(Instant periodStart, String channel, String status, long count) {
}
