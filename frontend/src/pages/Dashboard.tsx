import { useEffect, useRef } from 'react';
import { Title, useGetList, usePermissions, useTranslate } from 'react-admin';
import { Box, Button, Card, CardContent, CardHeader, Grid, List, ListItemButton, ListItemText, Stack, Typography } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd';
import { Link } from 'react-router-dom';
import { config } from '../config';
import { SCOPES } from '../api/constants';
import type { NotificationSummaryView } from '../api/types';
import { hasScope } from '../auth/authProvider';
import { ChannelChip } from '../components/ChannelChip';
import { StatusChip } from '../components/StatusChip';
import { StatisticsChart, StatisticsKpis, useStatistics } from './StatisticsPage';
import { useHealth } from '../channels/useChannelStatus';

const Recent = () => {
    const translate = useTranslate();
    const { data } = useGetList<NotificationSummaryView>('notifications', {
        pagination: { page: 1, perPage: 8 },
        sort: { field: 'createdAt', order: 'DESC' },
    });
    return (
        <Card variant="outlined" sx={{ height: '100%' }}>
            <CardHeader title={translate('app.dashboard.recent')} action={<Button component={Link} to="/notifications">{translate('app.dashboard.all')}</Button>} />
            <List dense>
                {(data ?? []).map(item => (
                    <ListItemButton key={item.id} component={Link} to={`/notifications/${item.id}/show`}>
                        <ListItemText
                            primary={
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <span>{item.recipientId}</span>
                                    {item.channels.map(channel => (
                                        <ChannelChip key={channel} channel={channel} />
                                    ))}
                                </Stack>
                            }
                            secondary={`${item.templateKey ?? translate('app.message.raw_content')} · ${new Date(item.createdAt).toLocaleString()}`}
                        />
                        <StatusChip status={item.status} />
                    </ListItemButton>
                ))}
                {data?.length === 0 && (
                    <Typography color="text.secondary" p={2}>
                        {translate('app.message.no_data')}
                    </Typography>
                )}
            </List>
        </Card>
    );
};

export const Dashboard = () => {
    const translate = useTranslate();
    const { permissions } = usePermissions();
    const stats = useStatistics(7);
    const health = useHealth();
    const canRead = hasScope(permissions, SCOPES.read);
    const canSend = hasScope(permissions, SCOPES.send);
    const { load } = stats;
    const loaded = useRef(false);
    // load is recreated on every render (the data provider proxy is not stable): run it once.
    useEffect(() => {
        if (canRead && !loaded.current) {
            loaded.current = true;
            load();
        }
    }, [canRead, load]);
    return (
        <Box p={2}>
            <Title title={config.title} />
            <Stack direction="row" spacing={1} mb={2} alignItems="center" useFlexGap flexWrap="wrap">
                <Typography variant="h5" sx={{ flexGrow: 1 }}>
                    {translate('app.dashboard.welcome')}
                </Typography>
                {health.data && (
                    <Button component={Link} to="/system" size="small">
                        {translate('app.system.health')}: <StatusChip status={health.data.status} sx={{ ml: 1 }} />
                    </Button>
                )}
                {canSend && (
                    <>
                        <Button variant="contained" startIcon={<SendIcon />} component={Link} to="/notifications/create">
                            {translate('app.action.send')}
                        </Button>
                        <Button variant="outlined" startIcon={<PlaylistAddIcon />} component={Link} to="/notifications/batch">
                            {translate('app.action.batch')}
                        </Button>
                    </>
                )}
            </Stack>
            {canRead && stats.data && <StatisticsKpis data={stats.data} />}
            {canRead && (
                <Grid container spacing={2} mt={0}>
                    <Grid size={{ xs: 12, lg: 7 }}>
                        <Card variant="outlined" sx={{ height: '100%' }}>
                            <CardHeader title={translate('app.dashboard.last_7_days')} />
                            <CardContent>{stats.data && <StatisticsChart data={stats.data} granularity="DAY" height={280} />}</CardContent>
                        </Card>
                    </Grid>
                    <Grid size={{ xs: 12, lg: 5 }}>
                        <Recent />
                    </Grid>
                </Grid>
            )}
        </Box>
    );
};
