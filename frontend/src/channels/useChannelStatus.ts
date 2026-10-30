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

const CB_STATE_METRIC = 'resilience4j.circuitbreaker.state';
const CB_FAILURE_RATE_METRIC = 'resilience4j.circuitbreaker.failure.rate';
// Most likely first: usually one request per breaker.
const CB_STATES = ['closed', 'open', 'half_open', 'forced_open', 'disabled', 'metrics_only'];

/**
 * Circuit breakers from Micrometer (resilience4j.circuitbreaker.state{name, state} = 1 for the
 * current state); a breaker channel-<channel> exists only for enabled channels. Falls back to
 * the health component when metrics are not reachable.
 */
export const useCircuitBreakers = () => {
    const api = useNotificationApi();
    const health = useHealth();
    const metrics = useQuery({
        queryKey: ['actuator', 'metrics', CB_STATE_METRIC],
        refetchInterval: 15000,
        queryFn: async (): Promise<Record<string, CircuitBreakerState> | null> => {
            const overall = await api.getMetric(CB_STATE_METRIC);
            if (!overall) return null;
            const names = overall.availableTags.find(t => t.tag === 'name')?.values ?? [];
            const result: Record<string, CircuitBreakerState> = {};
            await Promise.all(
                names.map(async name => {
                    let state: string | undefined;
                    for (const candidate of CB_STATES) {
                        const metric = await api.getMetric(CB_STATE_METRIC, { name, state: candidate });
                        if (metric?.measurements.some(m => m.value >= 1)) {
                            state = candidate.toUpperCase();
                            break;
                        }
                    }
                    const rate = (await api.getMetric(CB_FAILURE_RATE_METRIC, { name }))?.measurements[0]?.value;
                    result[name] = {
                        status: state === 'OPEN' || state === 'FORCED_OPEN' ? 'CIRCUIT_OPEN' : 'UP',
                        state,
                        // -1: not enough calls in the sliding window yet.
                        failureRate: rate === undefined || rate < 0 ? undefined : `${rate}%`,
                    };
                }),
            );
            return result;
        },
    });
    return { data: metrics.data ?? circuitBreakers(health.data), isPending: metrics.isPending && health.isPending };
};

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
