import { useQuery } from '@tanstack/react-query';
import { useTranslate } from 'react-admin';
import { Alert, Box, Chip, CircularProgress, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useNotificationApi } from '../api/useNotificationApi';
import type { DeliveryView, NotificationView, PageResponse, NotificationSummaryView } from '../api/types';
import { api as http } from '../api/http';
import { CodeBlock } from '../components/CodeBlock';
import type { ProviderDescriptor } from './types';

interface LogLine {
    notification: NotificationView;
    delivery: DeliveryView;
}

/** Line in the format of StubProviderSupport, reconstructed from the delivery record. */
export function formatLogLine({ delivery }: LogLine, channel: string): string {
    const time = delivery.sentAt ?? delivery.updatedAt;
    if (delivery.status === 'FAILED' || delivery.status === 'RETRY_SCHEDULED') {
        return `${time} WARN  Stub ${channel} provider simulated a failure deliveryId=${delivery.id} to=${delivery.maskedAddress ?? '?'} error="${delivery.lastError ?? ''}"`;
    }
    if (delivery.status === 'SKIPPED') {
        return `${time} INFO  Delivery ${delivery.id} skipped: ${delivery.skipReason}`;
    }
    if (!delivery.providerMessageId) {
        return `${time} INFO  Delivery ${delivery.id} is ${delivery.status}`;
    }
    return `${time} INFO  Stub ${channel} provider would send deliveryId=${delivery.id} to=${delivery.maskedAddress ?? '?'} → ${delivery.providerMessageId}`;
}

/** Recent deliveries of the channel, rendered like the stub's log; the real log is in the service container. */
export const LoggerConsole = ({ channel, provider }: { channel: string; provider: ProviderDescriptor }) => {
    const translate = useTranslate();
    const api = useNotificationApi();
    const { data, isPending, error } = useQuery({
        queryKey: ['logger-console', channel],
        refetchInterval: 10000,
        queryFn: async (): Promise<LogLine[]> => {
            const page = await http.get<PageResponse<NotificationSummaryView>>('/api/v1/notifications', { channel, size: 10, page: 0 });
            const notifications = await Promise.all(
                page.items.map(item => api.getOne<NotificationView>('notifications', { id: item.id }).then(r => r.data)),
            );
            return notifications.flatMap(notification =>
                notification.deliveries.filter(d => d.channel === channel).map(delivery => ({ notification, delivery })),
            );
        },
    });
    const logCommand = `docker compose logs -f notification-service | grep "Stub ${channel} provider"`;
    return (
        <Box>
            <Typography variant="body2" color="text.secondary" mb={1}>
                {translate('app.provider.console_help', { implementation: provider.implementation })}
            </Typography>
            {isPending && <CircularProgress size={20} />}
            {error && <Alert severity="error">{(error as Error).message}</Alert>}
            {data && (
                <Box
                    sx={{
                        bgcolor: '#0d1117',
                        color: '#c9d1d9',
                        fontFamily: 'ui-monospace, Menlo, monospace',
                        fontSize: 12,
                        p: 1.5,
                        borderRadius: 1,
                        maxHeight: 300,
                        overflow: 'auto',
                    }}
                >
                    {data.length === 0 && <Box sx={{ color: '#8b949e' }}>{translate('app.provider.console_empty')}</Box>}
                    {data.map(line => (
                        <Box key={line.delivery.id} sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', mb: 0.5 }}>
                            <Box component="span" sx={{ color: line.delivery.lastError ? '#f85149' : '#3fb950' }}>
                                {formatLogLine(line, channel)}
                            </Box>{' '}
                            <Link to={`/notifications/${line.notification.id}/show`} style={{ color: '#58a6ff' }}>
                                #
                            </Link>
                        </Box>
                    ))}
                </Box>
            )}
            <Stack direction="row" spacing={1} mt={1} useFlexGap flexWrap="wrap">
                {data?.some(line => line.delivery.simulated) && <Chip size="small" color="warning" label="simulated=true" />}
            </Stack>
            <Typography variant="subtitle2" mt={2}>
                {translate('app.provider.real_log')}
            </Typography>
            <CodeBlock code={logCommand} />
        </Box>
    );
};
