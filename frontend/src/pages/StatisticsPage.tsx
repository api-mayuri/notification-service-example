import { useCallback, useEffect, useMemo, useState } from 'react';
import { Title, useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    FormControl,
    Grid,
    InputLabel,
    LinearProgress,
    MenuItem,
    Select,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { CHANNELS, DELIVERY_STATUSES, LIMITS } from '../api/constants';
import { useNotificationApi } from '../api/useNotificationApi';
import type { Granularity, StatisticsView } from '../api/types';
import { ChannelChip } from '../components/ChannelChip';

export const STATUS_PALETTE: Record<string, string> = {
    PENDING: '#90caf9',
    SENDING: '#42a5f5',
    RETRY_SCHEDULED: '#ffb74d',
    SENT: '#66bb6a',
    DELIVERED: '#2e7d32',
    FAILED: '#e53935',
    SKIPPED: '#9e9e9e',
    CANCELLED: '#bdbdbd',
};

/** Rows (period, channel, status, count) → one chart point per period with a key per status. */
export function toChartData(stats: StatisticsView) {
    const points = new Map<string, Record<string, number | string>>();
    for (const row of stats.rows) {
        const point = points.get(row.periodStart) ?? { period: row.periodStart };
        point[row.status] = ((point[row.status] as number | undefined) ?? 0) + row.count;
        points.set(row.periodStart, point);
    }
    return [...points.values()].sort((a, b) => String(a.period).localeCompare(String(b.period)));
}

export function summarize(totals: StatisticsView['totals']) {
    const sum = (statuses: string[]) =>
        Object.values(totals).reduce((acc, byStatus) => acc + statuses.reduce((s, status) => s + (byStatus[status] ?? 0), 0), 0);
    const all = Object.values(totals).reduce((acc, byStatus) => acc + Object.values(byStatus).reduce((s, n) => s + n, 0), 0);
    const delivered = sum(['SENT', 'DELIVERED']);
    const failed = sum(['FAILED']);
    return {
        total: all,
        delivered,
        failed,
        skipped: sum(['SKIPPED']),
        inFlight: sum(['PENDING', 'SENDING', 'RETRY_SCHEDULED']),
        successRate: delivered + failed ? Math.round((delivered / (delivered + failed)) * 1000) / 10 : null,
    };
}

const toLocalInput = (date: Date) => {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
};

export const useStatistics = (initialDays = 7) => {
    const api = useNotificationApi();
    const [from, setFrom] = useState(() => toLocalInput(new Date(Date.now() - initialDays * 86400000)));
    const [to, setTo] = useState(() => toLocalInput(new Date()));
    const [channel, setChannel] = useState('');
    const [granularity, setGranularity] = useState<Granularity>('DAY');
    const [data, setData] = useState<StatisticsView | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const load = useCallback(async () => {
        const days = (new Date(to).getTime() - new Date(from).getTime()) / 86400000;
        if (days <= 0) return setError('app.validation.period_order');
        if (days > LIMITS.statisticsDays) return setError('app.validation.period_too_long');
        setLoading(true);
        setError(null);
        try {
            setData(await api.getStatistics({ from, to, channel: channel || undefined, granularity }));
        } catch (loadError) {
            setError((loadError as Error).message);
        } finally {
            setLoading(false);
        }
    }, [api, from, to, channel, granularity]);

    return { from, setFrom, to, setTo, channel, setChannel, granularity, setGranularity, data, error, loading, load };
};

export const StatisticsChart = ({ data, granularity, height = 320 }: { data: StatisticsView; granularity: Granularity; height?: number }) => {
    const translate = useTranslate();
    const points = useMemo(() => toChartData(data), [data]);
    const statuses = DELIVERY_STATUSES.filter(status => points.some(point => point[status]));
    const formatPeriod = (value: string) => {
        const date = new Date(value);
        return granularity === 'HOUR'
            ? date.toLocaleString(undefined, { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
            : date.toLocaleDateString(undefined, { day: '2-digit', month: '2-digit' });
    };
    if (!points.length) {
        return (
            <Typography sx={{ textAlign: 'center', py: 4 }} color="text.secondary">
                {translate('app.message.no_data')}
            </Typography>
        );
    }
    return (
        <ResponsiveContainer width="100%" height={height}>
            <BarChart data={points}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="period" tickFormatter={formatPeriod} />
                <YAxis allowDecimals={false} />
                <Tooltip labelFormatter={label => formatPeriod(String(label))} />
                <Legend formatter={value => translate(`app.status.${value}`, { _: value })} />
                {statuses.map(status => (
                    <Bar key={status} dataKey={status} stackId="status" fill={STATUS_PALETTE[status]} />
                ))}
            </BarChart>
        </ResponsiveContainer>
    );
};

const Kpi = ({ label, value, color }: { label: string; value: string | number; color?: string }) => (
    <Card variant="outlined" sx={{ height: '100%' }}>
        <CardContent>
            <Typography variant="caption" color="text.secondary">
                {label}
            </Typography>
            <Typography variant="h4" color={color}>
                {value}
            </Typography>
        </CardContent>
    </Card>
);

export const StatisticsKpis = ({ data }: { data: StatisticsView }) => {
    const translate = useTranslate();
    const summary = summarize(data.totals);
    return (
        <Grid container spacing={2}>
            <Grid size={{ xs: 6, md: 2.4 }}>
                <Kpi label={translate('app.stats.total')} value={summary.total} />
            </Grid>
            <Grid size={{ xs: 6, md: 2.4 }}>
                <Kpi label={translate('app.stats.delivered')} value={summary.delivered} color="success.main" />
            </Grid>
            <Grid size={{ xs: 6, md: 2.4 }}>
                <Kpi label={translate('app.stats.failed')} value={summary.failed} color="error.main" />
            </Grid>
            <Grid size={{ xs: 6, md: 2.4 }}>
                <Kpi label={translate('app.stats.in_flight')} value={summary.inFlight} color="info.main" />
            </Grid>
            <Grid size={{ xs: 12, md: 2.4 }}>
                <Kpi label={translate('app.stats.success_rate')} value={summary.successRate === null ? '—' : `${summary.successRate}%`} />
            </Grid>
        </Grid>
    );
};

export const StatisticsPage = () => {
    const translate = useTranslate();
    const stats = useStatistics();
    const { load } = stats;
    useEffect(() => {
        load();
    }, []); // first load only; later loads by the Apply button
    const statuses = stats.data
        ? DELIVERY_STATUSES.filter(status => Object.values(stats.data!.totals).some(byStatus => byStatus[status]))
        : [];
    return (
        <Box sx={{ p: 2 }}>
            <Title title={translate('app.menu.statistics')} />
            <Card variant="outlined" sx={{ mb: 2 }}>
                <CardContent>
                    <Stack sx={{ alignItems: { md: 'center' } }} direction={{ xs: 'column', md: 'row' }} spacing={2}>
                        <TextField
                            type="datetime-local"
                            size="small"
                            label={translate('app.field.from')}
                            value={stats.from}
                            onChange={event => stats.setFrom(event.target.value)}
                            slotProps={{ inputLabel: { shrink: true } }}
                        />
                        <TextField
                            type="datetime-local"
                            size="small"
                            label={translate('app.field.to')}
                            value={stats.to}
                            onChange={event => stats.setTo(event.target.value)}
                            slotProps={{ inputLabel: { shrink: true } }}
                        />
                        <FormControl size="small" sx={{ minWidth: 160 }}>
                            <InputLabel>{translate('app.field.channel')}</InputLabel>
                            <Select label={translate('app.field.channel')} value={stats.channel} onChange={event => stats.setChannel(event.target.value)}>
                                <MenuItem value="">{translate('app.field.all_channels')}</MenuItem>
                                {CHANNELS.map(id => (
                                    <MenuItem key={id} value={id}>
                                        {id}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        <ToggleButtonGroup
                            size="small"
                            exclusive
                            value={stats.granularity}
                            onChange={(_, value) => value && stats.setGranularity(value)}
                        >
                            <ToggleButton value="HOUR">{translate('app.stats.hour')}</ToggleButton>
                            <ToggleButton value="DAY">{translate('app.stats.day')}</ToggleButton>
                        </ToggleButtonGroup>
                        <Button variant="contained" startIcon={<RefreshIcon />} onClick={stats.load}>
                            {translate('app.action.apply')}
                        </Button>
                    </Stack>
                    <Typography sx={{ display: 'block', mt: 1 }} variant="caption" color="text.secondary">
                        {translate('app.help.statistics', { days: LIMITS.statisticsDays })}
                    </Typography>
                </CardContent>
                {stats.loading && <LinearProgress />}
            </Card>
            {stats.error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {translate(stats.error, { _: stats.error, days: LIMITS.statisticsDays })}
                </Alert>
            )}
            {stats.data && (
                <>
                    <StatisticsKpis data={stats.data} />
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardContent>
                            <Typography variant="h6" gutterBottom>
                                {translate('app.section.deliveries_by_period')}
                            </Typography>
                            <StatisticsChart data={stats.data} granularity={stats.data.granularity} />
                        </CardContent>
                    </Card>
                    <Card variant="outlined" sx={{ mt: 2 }}>
                        <CardContent>
                            <Typography variant="h6" gutterBottom>
                                {translate('app.section.totals_by_channel')}
                            </Typography>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>{translate('app.field.channel')}</TableCell>
                                        {statuses.map(status => (
                                            <TableCell key={status} align="right">
                                                {translate(`app.status.${status}`, { _: status })}
                                            </TableCell>
                                        ))}
                                        <TableCell align="right">{translate('app.stats.total')}</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {Object.entries(stats.data.totals).map(([channel, byStatus]) => (
                                        <TableRow key={channel}>
                                            <TableCell>
                                                <ChannelChip channel={channel} />
                                            </TableCell>
                                            {statuses.map(status => (
                                                <TableCell key={status} align="right">
                                                    {byStatus[status] ?? 0}
                                                </TableCell>
                                            ))}
                                            <TableCell align="right">
                                                <b>{Object.values(byStatus).reduce((s, n) => s + n, 0)}</b>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>
                </>
            )}
        </Box>
    );
};
