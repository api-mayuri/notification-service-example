import { useQuery } from '@tanstack/react-query';
import { useNotificationApi } from '../api/useNotificationApi';
import type { HealthComponent } from '../api/types';

export interface CircuitBreakerState {
    status: string;
    state?: string;
    failureRate?: string;
    bufferedCalls?: number;
    failedCalls?: number;
}

export interface ChannelMetrics {
    /** outcome tag (success, simulated, retryable_failure, permanent_failure, error, circuit_open) → calls. */
    outcomes: Record<string, number>;
    total: number;
    /** Average send duration, ms. */
    avgMs: number | null;
}

export const METRIC = 'notification.channel.send';

/** Circuit breakers from /actuator/health (details are shown in profile local). */
export function circuitBreakers(health: HealthComponent | undefined): Record<string, CircuitBreakerState> | null {
    const component = health?.components?.circuitBreakers;
    if (!component?.details) return null;
    const result: Record<string, CircuitBreakerState> = {};
    for (const [name, value] of Object.entries(component.details)) {
        const entry = value as { status?: string; details?: Record<string, unknown> };
        result[name] = {
            status: entry.status ?? 'UNKNOWN',
            state: entry.details?.state as string | undefined,
            failureRate: entry.details?.failureRate as string | undefined,
            bufferedCalls: entry.details?.bufferedCalls as number | undefined,
            failedCalls: entry.details?.failedCalls as number | undefined,
        };
    }
    return result;
}

export const useHealth = () => {
    const api = useNotificationApi();
    return useQuery({ queryKey: ['actuator', 'health'], queryFn: () => api.getHealth(), refetchInterval: 15000 });
};

/** Micrometer meter notification.channel.send{channel, outcome} — recorded by ResilientChannelSender. */
export const useChannelMetrics = (channel: string) => {
    const api = useNotificationApi();
    const tag = channel.toLowerCase();
    return useQuery({
        queryKey: ['actuator', 'metrics', METRIC, tag],
        refetchInterval: 15000,
        queryFn: async (): Promise<ChannelMetrics> => {
            const overall = await api.getMetric(METRIC, { channel: tag });
            if (!overall) return { outcomes: {}, total: 0, avgMs: null };
            const outcomes = overall.availableTags.find(t => t.tag === 'outcome')?.values ?? [];
            const counts: Record<string, number> = {};
            await Promise.all(
                outcomes.map(async outcome => {
                    const metric = await api.getMetric(METRIC, { channel: tag, outcome });
                    counts[outcome] = metric?.measurements.find(m => m.statistic === 'COUNT')?.value ?? 0;
                }),
            );
            const total = overall.measurements.find(m => m.statistic === 'COUNT')?.value ?? 0;
            const totalTime = overall.measurements.find(m => m.statistic === 'TOTAL_TIME')?.value ?? 0;
            // TOTAL_TIME is in base unit "seconds".
            return { outcomes: counts, total, avgMs: total ? Math.round((totalTime / total) * 1000 * 10) / 10 : null };
        },
    });
};
