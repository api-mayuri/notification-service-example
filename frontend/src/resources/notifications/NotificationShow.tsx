import {
    ArrayField,
    Datagrid,
    DateField,
    FunctionField,
    Labeled,
    ListButton,
    NumberField,
    RefreshButton,
    Show,
    TextField,
    TopToolbar,
    usePermissions,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { Box, Card, CardContent, Chip, Grid, Link as MuiLink, Stack, Tooltip, Typography } from '@mui/material';
import ScienceIcon from '@mui/icons-material/Science';
import { Link } from 'react-router-dom';
import { config } from '../../config';
import { SCOPES, TERMINAL_NOTIFICATION_STATUSES } from '../../api/constants';
import type { DeliveryView, NotificationView } from '../../api/types';
import { hasScope } from '../../auth/authProvider';
import { ChannelChip, ChannelsField } from '../../components/ChannelChip';
import { CopyButton } from '../../components/CopyButton';
import { StatusChip, StatusField } from '../../components/StatusChip';
import { CancelButton } from './CancelButton';

const ShowActions = () => {
    const { permissions } = usePermissions();
    return (
        <TopToolbar>
            <ListButton />
            <RefreshButton />
            {hasScope(permissions, SCOPES.send) && <CancelButton />}
        </TopToolbar>
    );
};

const Title = () => {
    const record = useRecordContext<NotificationView>();
    const translate = useTranslate();
    return <span>{translate('app.page.notification', { id: record?.id ?? '' })}</span>;
};

const DeliveryProgress = () => {
    const record = useRecordContext<NotificationView>();
    const translate = useTranslate();
    if (!record) return null;
    const counts = (record.deliveries ?? []).reduce<Record<string, number>>((acc, delivery) => {
        acc[delivery.status] = (acc[delivery.status] ?? 0) + 1;
        return acc;
    }, {});
    const live = !TERMINAL_NOTIFICATION_STATUSES.includes(record.status) && config.pollIntervalMs > 0;
    return (
        <Stack sx={{ alignItems: 'center', flexWrap: 'wrap' }} direction="row" spacing={1} useFlexGap>
            <StatusChip status={record.status} size="medium" />
            {Object.entries(counts).map(([status, count]) => (
                <Chip key={status} size="small" variant="outlined" label={`${translate(`app.status.${status}`, { _: status })}: ${count}`} />
            ))}
            {live && (
                <Typography variant="caption" color="text.secondary">
                    {translate('app.message.auto_refresh', { seconds: Math.round(config.pollIntervalMs / 1000) })}
                </Typography>
            )}
        </Stack>
    );
};

const Summary = () => {
    const record = useRecordContext<NotificationView>();
    const { permissions } = usePermissions();
    if (!record) return null;
    return (
        <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
                <Labeled label="app.field.id">
                    <Stack sx={{ alignItems: 'center' }} direction="row">
                        <TextField source="id" sx={{ fontFamily: 'monospace' }} />
                        <CopyButton text={record.id} />
                    </Stack>
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.clientId">
                    <TextField source="clientId" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.recipientId">
                    {hasScope(permissions, SCOPES.preferences) ? (
                        <MuiLink component={Link} to={`/preferences?recipientId=${encodeURIComponent(record.recipientId)}`}>
                            {record.recipientId}
                        </MuiLink>
                    ) : (
                        <TextField source="recipientId" />
                    )}
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.category">
                    <TextField source="category" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.priority">
                    <TextField source="priority" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <Labeled label="app.field.channels">
                    <ChannelsField source="channels" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.templateKey">
                    <TextField source="templateKey" emptyText="app.message.raw_content" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.sendAt">
                    <DateField source="sendAt" showTime emptyText="—" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.createdAt">
                    <DateField source="createdAt" showTime />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.updatedAt">
                    <DateField source="updatedAt" showTime />
                </Labeled>
            </Grid>
            {record.metadata && Object.keys(record.metadata).length > 0 && (
                <Grid size={12}>
                    <Labeled label="app.field.metadata">
                        <Stack sx={{ flexWrap: 'wrap' }} direction="row" spacing={1} useFlexGap>
                            {Object.entries(record.metadata).map(([key, value]) => (
                                <Chip key={key} size="small" label={`${key}: ${value}`} />
                            ))}
                        </Stack>
                    </Labeled>
                </Grid>
            )}
        </Grid>
    );
};

const ErrorOrSkip = (_props: { label?: string }) => {
    const record = useRecordContext<DeliveryView>();
    const translate = useTranslate();
    if (!record) return null;
    if (record.skipReason) {
        return <Chip size="small" variant="outlined" label={translate(`app.skip.${record.skipReason}`, { _: record.skipReason })} />;
    }
    return record.lastError ? (
        <Typography variant="body2" color="error" sx={{ maxWidth: 320, wordBreak: 'break-word' }}>
            {record.lastError}
        </Typography>
    ) : null;
};

const ProviderMessage = (_props: { label?: string }) => {
    const record = useRecordContext<DeliveryView>();
    const translate = useTranslate();
    if (!record?.providerMessageId) return null;
    return (
        <Stack sx={{ alignItems: 'center' }} direction="row" spacing={0.5}>
            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: 12 }}>
                {record.providerMessageId}
            </Typography>
            {record.simulated && (
                <Tooltip title={translate('app.message.simulated_hint')}>
                    <Chip size="small" color="warning" icon={<ScienceIcon />} label={translate('app.field.simulated')} />
                </Tooltip>
            )}
        </Stack>
    );
};

export const NotificationShow = () => (
    <Show
        actions={<ShowActions />}
        title={<Title />}
        queryOptions={{
            refetchInterval: query => {
                const status = (query.state.data as NotificationView | undefined)?.status;
                return config.pollIntervalMs > 0 && status && !TERMINAL_NOTIFICATION_STATUSES.includes(status)
                    ? config.pollIntervalMs
                    : false;
            },
        }}
    >
        <Card variant="outlined" sx={{ m: 2 }}>
            <CardContent>
                <DeliveryProgress />
                <Box sx={{ mt: 2 }}>
                    <Summary />
                </Box>
            </CardContent>
        </Card>
        <Box sx={{ px: 2, pb: 2 }}>
            <Typography variant="h6" gutterBottom>
                <DeliveriesTitle />
            </Typography>
            <ArrayField source="deliveries">
                <Datagrid bulkActionButtons={false} rowClick={false} hover={false}>
                    <FunctionField label="app.field.channel" render={(d: DeliveryView) => <ChannelChip channel={d.channel} />} />
                    <StatusField source="status" label="app.field.status" />
                    <TextField source="maskedAddress" label="app.field.address" emptyText="—" />
                    <NumberField source="attempts" label="app.field.attempts" />
                    <DateField source="nextAttemptAt" label="app.field.nextAttemptAt" showTime emptyText="—" />
                    <ErrorOrSkip label="app.field.lastError" />
                    <ProviderMessage label="app.field.providerMessageId" />
                    <FunctionField
                        label="app.field.templateVersion"
                        render={(d: DeliveryView) => (d.templateVersion ? `v${d.templateVersion} · ${d.templateLocale ?? ''}` : '—')}
                    />
                    <DateField source="sentAt" label="app.field.sentAt" showTime emptyText="—" />
                    <DateField source="updatedAt" label="app.field.updatedAt" showTime />
                </Datagrid>
            </ArrayField>
        </Box>
    </Show>
);

const DeliveriesTitle = () => {
    const translate = useTranslate();
    return <>{translate('app.section.deliveries')}</>;
};
