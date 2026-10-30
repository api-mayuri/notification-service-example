import { useState } from 'react';
import { Title, useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    Card,
    CardActions,
    CardContent,
    CardHeader,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Grid,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Tooltip,
    Typography,
} from '@mui/material';
import TerminalIcon from '@mui/icons-material/Terminal';
import PowerIcon from '@mui/icons-material/Power';
import SendIcon from '@mui/icons-material/Send';
import VerifiedIcon from '@mui/icons-material/VerifiedUser';
import { useNavigate } from 'react-router-dom';
import { CHANNELS } from '../api/constants';
import { channelIcon } from '../components/ChannelChip';
import { StatusChip } from '../components/StatusChip';
import { ConnectProviderDialog, ProviderBadges } from '../channels/ConnectProviderDialog';
import { LoggerConsole } from '../channels/LoggerConsole';
import { WebhookSignatureTool } from '../channels/WebhookSignatureTool';
import { channelInfo, currentProviderId, findProvider, providerEnv, providersFor } from '../channels/registry';
import { useChannelMetrics, useCircuitBreakers, type CircuitBreakerState } from '../channels/useChannelStatus';

const OUTCOME_COLORS: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
    success: 'success',
    simulated: 'info',
    retryable_failure: 'warning',
    permanent_failure: 'error',
    error: 'error',
    circuit_open: 'error',
};

const Metrics = ({ channel }: { channel: string }) => {
    const translate = useTranslate();
    const { data, error } = useChannelMetrics(channel);
    if (error) {
        return (
            <Typography variant="caption" color="text.secondary">
                {translate('app.provider.metrics_unavailable')}
            </Typography>
        );
    }
    if (!data) return null;
    if (!data.total) {
        return (
            <Typography variant="caption" color="text.secondary">
                {translate('app.provider.no_sends')}
            </Typography>
        );
    }
    return (
        <Stack sx={{ flexWrap: 'wrap', alignItems: 'center' }} direction="row" spacing={0.5} useFlexGap>
            {Object.entries(data.outcomes).map(([outcome, count]) => (
                <Chip
                    key={outcome}
                    size="small"
                    variant="outlined"
                    color={OUTCOME_COLORS[outcome] ?? 'default'}
                    label={`${translate(`app.outcome_metric.${outcome}`, { _: outcome })}: ${count}`}
                />
            ))}
            {data.avgMs !== null && (
                <Typography variant="caption" color="text.secondary">
                    ⌀ {data.avgMs} ms
                </Typography>
            )}
        </Stack>
    );
};

const ChannelCard = ({ channel, breaker, breakersKnown }: { channel: string; breaker?: CircuitBreakerState; breakersKnown: boolean }) => {
    const translate = useTranslate();
    const navigate = useNavigate();
    const [dialog, setDialog] = useState<'connect' | 'console' | 'signature' | null>(null);
    const info = channelInfo(channel);
    const providerId = currentProviderId(channel);
    const provider = findProvider(channel, providerId);
    const enabled = breakersKnown ? Boolean(breaker) : undefined;
    const sendTest = () =>
        navigate(`/notifications/create?source=${encodeURIComponent(JSON.stringify({ channels: [channel] }))}`);

    return (
        <Card variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column', opacity: enabled === false ? 0.6 : 1 }}>
            <CardHeader
                avatar={channelIcon(channel)}
                title={<Typography variant="h6">{channel}</Typography>}
                subheader={translate(info.description, { _: channel })}
                action={
                    <Stack sx={{ mt: 1 }} direction="row" spacing={0.5}>
                        {enabled === undefined ? (
                            <Chip size="small" label={translate('app.provider.state_unknown')} />
                        ) : enabled ? (
                            <Chip size="small" color="success" label={translate('app.provider.enabled')} />
                        ) : (
                            <Chip size="small" label={translate('app.provider.disabled')} />
                        )}
                        {breaker?.state && (
                            <Tooltip title={`Circuit breaker channel-${channel.toLowerCase()} · failure rate ${breaker.failureRate ?? '—'}`}>
                                <span>
                                    <StatusChip status={breaker.state} />
                                </span>
                            </Tooltip>
                        )}
                    </Stack>
                }
            />
            <CardContent sx={{ flexGrow: 1, pt: 0 }}>
                <Typography sx={{ display: 'block' }} variant="overline" color="text.secondary">
                    {translate('app.provider.current_provider')}
                </Typography>
                {provider ? (
                    <Box sx={{ mb: 1 }}>
                        <Typography sx={{ fontWeight: 600 }} variant="subtitle1">
                            {provider.name}{' '}
                            <Typography component="span" variant="caption" color="text.secondary">
                                ({provider.id} · {provider.implementation})
                            </Typography>
                        </Typography>
                        <ProviderBadges provider={provider} />
                        <Typography sx={{ mt: 1 }} variant="body2" color="text.secondary">
                            {translate(provider.description)}
                        </Typography>
                    </Box>
                ) : (
                    <Alert severity="info" sx={{ mb: 1 }}>
                        {translate('app.provider.unknown_provider', { provider: providerId ?? '—' })}
                    </Alert>
                )}
                {provider?.kind === 'stub' && (
                    <Alert severity="warning" icon={<TerminalIcon />} sx={{ my: 1 }}>
                        {translate('app.provider.stub_notice')}
                    </Alert>
                )}
                <Divider sx={{ my: 1 }} />
                <Table size="small">
                    <TableBody>
                        <TableRow>
                            <TableCell sx={{ pl: 0 }}>{translate('app.provider.contact')}</TableCell>
                            <TableCell>
                                <code>recipient.contacts.{info.contactType}</code>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell sx={{ pl: 0 }}>{translate('app.provider.port')}</TableCell>
                            <TableCell>
                                <code>{info.providerInterface}</code>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell sx={{ pl: 0 }}>{translate('app.provider.selector')}</TableCell>
                            <TableCell>
                                <code>{providerEnv(channel)}</code>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell sx={{ pl: 0 }}>{translate('app.provider.available')}</TableCell>
                            <TableCell>{providersFor(channel).map(p => p.id).join(', ') || '—'}</TableCell>
                        </TableRow>
                    </TableBody>
                </Table>
                <Box sx={{ mt: 1 }}>
                    <Typography sx={{ display: 'block' }} variant="overline" color="text.secondary">
                        {translate('app.provider.metrics')}
                    </Typography>
                    <Metrics channel={channel} />
                </Box>
            </CardContent>
            <CardActions sx={{ flexWrap: 'wrap', gap: 0.5 }}>
                <Button size="small" startIcon={<PowerIcon />} onClick={() => setDialog('connect')} disabled={!providersFor(channel).length}>
                    {translate('app.provider.connect')}
                </Button>
                {provider?.kind === 'stub' && (
                    <Button size="small" startIcon={<TerminalIcon />} onClick={() => setDialog('console')}>
                        {translate('app.provider.console')}
                    </Button>
                )}
                {channel === 'WEBHOOK' && (
                    <Button size="small" startIcon={<VerifiedIcon />} onClick={() => setDialog('signature')}>
                        {translate('app.webhook.tool')}
                    </Button>
                )}
                <Button size="small" startIcon={<SendIcon />} onClick={sendTest} disabled={enabled === false}>
                    {translate('app.provider.send_test')}
                </Button>
            </CardActions>
            {dialog === 'connect' && <ConnectProviderDialog channel={channel} open onClose={() => setDialog(null)} />}
            {(dialog === 'console' || dialog === 'signature') && (
                <Dialog open onClose={() => setDialog(null)} fullWidth maxWidth="md">
                    <DialogTitle>
                        {dialog === 'console'
                            ? translate('app.provider.console_title', { channel })
                            : translate('app.webhook.tool')}
                    </DialogTitle>
                    <DialogContent>
                        {dialog === 'console' && provider ? <LoggerConsole channel={channel} provider={provider} /> : <WebhookSignatureTool />}
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setDialog(null)}>{translate('ra.action.close')}</Button>
                    </DialogActions>
                </Dialog>
            )}
        </Card>
    );
};

export const ChannelsPage = () => {
    const translate = useTranslate();
    const { data: breakers, isPending } = useCircuitBreakers();
    return (
        <Box sx={{ p: 2 }}>
            <Title title={translate('app.menu.channels')} />
            <Alert severity="info" sx={{ mb: 2 }}>
                {translate('app.provider.page_help')}
            </Alert>
            {!breakers && !isPending && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                    {translate('app.provider.no_health_details')}
                </Alert>
            )}
            <Grid container spacing={2}>
                {CHANNELS.map(channel => (
                    <Grid key={channel} size={{ xs: 12, md: 6, xl: 4 }}>
                        <ChannelCard channel={channel} breaker={breakers?.[`channel-${channel.toLowerCase()}`]} breakersKnown={Boolean(breakers)} />
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
};
