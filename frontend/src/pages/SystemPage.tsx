import { useQuery } from '@tanstack/react-query';
import { Title, useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CardHeader,
    Chip,
    Grid,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { config } from '../config';
import { ALL_SCOPES } from '../api/constants';
import type { HealthComponent } from '../api/types';
import { useNotificationApi } from '../api/useNotificationApi';
import { decodeJwt, tokenStorage } from '../auth/tokenStorage';
import { CodeBlock } from '../components/CodeBlock';
import { CopyButton } from '../components/CopyButton';
import { StatusChip } from '../components/StatusChip';
import { useHealth } from '../channels/useChannelStatus';

/** Kafka contracts (docs/asyncapi.yaml): public topics and event types. */
export const KAFKA_TOPICS = [
    { topic: 'notification.send.requests.v1', direction: 'in', description: 'app.kafka.send_requests' },
    { topic: 'notification.send.requests.v1-dlt', direction: 'out', description: 'app.kafka.send_requests_dlt' },
    { topic: 'notification.events.v1', direction: 'out', description: 'app.kafka.events' },
    { topic: 'notification.delivery.<channel>[.high].v1', direction: 'internal', description: 'app.kafka.delivery' },
    { topic: 'notification.delivery.<channel>…-retry-N / -dlt', direction: 'internal', description: 'app.kafka.retry' },
];

export const EVENT_TYPES = [
    'notification.accepted.v1',
    'notification.completed.v1',
    'notification.cancelled.v1',
    'delivery.scheduled.v1',
    'delivery.sent.v1',
    'delivery.retry-scheduled.v1',
    'delivery.failed.v1',
    'delivery.skipped.v1',
    'delivery.cancelled.v1',
].map(type => `kz.company.notification.${type}`);

const HealthTree = ({ name, component, depth = 0 }: { name: string; component: HealthComponent; depth?: number }) => (
    <>
        <TableRow>
            <TableCell sx={{ pl: 2 + depth * 3 }}>{name}</TableCell>
            <TableCell>
                <StatusChip status={component.status} />
            </TableCell>
            <TableCell>
                {component.details && (
                    <Typography variant="caption" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        {Object.entries(component.details)
                            .filter(([, value]) => typeof value !== 'object')
                            .map(([key, value]) => `${key}=${value}`)
                            .join('  ')}
                    </Typography>
                )}
            </TableCell>
        </TableRow>
        {Object.entries(component.components ?? {}).map(([child, value]) => (
            <HealthTree key={child} name={child} component={value} depth={depth + 1} />
        ))}
        {name === 'circuitBreakers' &&
            Object.entries(component.details ?? {}).map(([breaker, value]) => (
                <HealthTree key={breaker} name={breaker} component={value as HealthComponent} depth={depth + 1} />
            ))}
    </>
);

const TokenCard = () => {
    const translate = useTranslate();
    const auth = tokenStorage.get();
    if (!auth) return null;
    let claims: Record<string, unknown> = {};
    try {
        claims = decodeJwt(auth.token);
    } catch {
        // opaque token
    }
    return (
        <Card variant="outlined">
            <CardHeader title={translate('app.system.token')} action={<CopyButton text={auth.token} />} />
            <CardContent>
                <Stack sx={{ mb: 1, flexWrap: 'wrap' }} direction="row" spacing={1} useFlexGap>
                    <Chip label={`client: ${auth.clientId}`} color="primary" />
                    {auth.expiresAt && <Chip label={`${translate('app.system.expires')}: ${new Date(auth.expiresAt).toLocaleString()}`} />}
                </Stack>
                <Typography variant="subtitle2">{translate('app.system.scopes')}</Typography>
                <Stack sx={{ mb: 2, flexWrap: 'wrap' }} direction="row" spacing={0.5} useFlexGap>
                    {ALL_SCOPES.map(scope => (
                        <Chip
                            key={scope}
                            size="small"
                            color={auth.scopes.includes(scope) ? 'success' : 'default'}
                            variant={auth.scopes.includes(scope) ? 'filled' : 'outlined'}
                            label={scope}
                        />
                    ))}
                </Stack>
                <CodeBlock code={JSON.stringify(claims, null, 2)} maxHeight={220} />
            </CardContent>
        </Card>
    );
};

export const SystemPage = () => {
    const translate = useTranslate();
    const api = useNotificationApi();
    const health = useHealth();
    const info = useQuery({ queryKey: ['actuator', 'info'], queryFn: () => api.getInfo() });
    const links = [
        { label: 'Swagger UI', href: config.swaggerUrl },
        { label: 'OpenAPI JSON', href: `${config.apiUrl}/v3/api-docs` },
        { label: 'Mailpit', href: config.mailpitUrl },
        { label: 'Kafka UI', href: config.kafkaUiUrl },
        { label: 'Prometheus', href: `${config.apiUrl}/actuator/prometheus` },
    ];
    return (
        <Box sx={{ p: 2 }}>
            <Title title={translate('app.menu.system')} />
            <Grid container spacing={2}>
                <Grid size={{ xs: 12, lg: 7 }}>
                    <Card variant="outlined">
                        <CardHeader
                            title={translate('app.system.health')}
                            action={health.data && <StatusChip status={health.data.status} size="medium" />}
                        />
                        <CardContent>
                            {health.error && <Alert severity="error">{(health.error as Error).message}</Alert>}
                            {health.data && (
                                <Table size="small">
                                    <TableBody>
                                        {Object.entries(health.data.components ?? {}).map(([name, component]) => (
                                            <HealthTree key={name} name={name} component={component} />
                                        ))}
                                    </TableBody>
                                </Table>
                            )}
                            {health.data && !health.data.components && (
                                <Typography variant="body2" color="text.secondary">
                                    {translate('app.provider.no_health_details')}
                                </Typography>
                            )}
                        </CardContent>
                    </Card>
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardHeader title={translate('app.system.kafka')} />
                        <CardContent>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>{translate('app.system.topic')}</TableCell>
                                        <TableCell>{translate('app.system.direction')}</TableCell>
                                        <TableCell>{translate('app.system.purpose')}</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {KAFKA_TOPICS.map(topic => (
                                        <TableRow key={topic.topic}>
                                            <TableCell sx={{ fontFamily: 'monospace' }}>{topic.topic}</TableCell>
                                            <TableCell>
                                                <Chip size="small" label={translate(`app.kafka.direction.${topic.direction}`)} />
                                            </TableCell>
                                            <TableCell>{translate(topic.description)}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                            <Typography sx={{ mt: 2 }} variant="subtitle2">
                                {translate('app.system.event_types')}
                            </Typography>
                            <CodeBlock code={EVENT_TYPES.join('\n')} />
                            <Button size="small" href={config.kafkaUiUrl} target="_blank" rel="noreferrer" endIcon={<OpenInNewIcon />} sx={{ mt: 1 }}>
                                Kafka UI
                            </Button>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid size={{ xs: 12, lg: 5 }}>
                    <TokenCard />
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardHeader title={translate('app.system.links')} />
                        <CardContent>
                            <Stack sx={{ alignItems: 'flex-start' }} spacing={1}>
                                {links.map(link => (
                                    <Button key={link.label} href={link.href} target="_blank" rel="noreferrer" endIcon={<OpenInNewIcon />}>
                                        {link.label}
                                    </Button>
                                ))}
                            </Stack>
                        </CardContent>
                    </Card>
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardHeader title={translate('app.system.info')} />
                        <CardContent>
                            {info.data && <CodeBlock code={JSON.stringify(info.data, null, 2)} maxHeight={200} />}
                            {info.error && <Alert severity="warning">{(info.error as Error).message}</Alert>}
                        </CardContent>
                    </Card>
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardHeader title={translate('app.system.frontend_config')} />
                        <CardContent>
                            <CodeBlock code={JSON.stringify(config, null, 2)} maxHeight={260} />
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>
        </Box>
    );
};
