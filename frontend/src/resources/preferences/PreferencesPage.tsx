import { useEffect, useMemo, useState } from 'react';
import {
    AutocompleteInput,
    CheckboxGroupInput,
    Form,
    SaveButton,
    TextInput,
    TimeInput,
    Title,
    useGetOne,
    useNotify,
    useTranslate,
    useUpdate,
} from 'react-admin';
import { useSearchParams } from 'react-router-dom';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Grid,
    InputAdornment,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useQueryClient } from '@tanstack/react-query';
import { CATEGORIES, CHANNELS, OPTIONAL_CATEGORIES } from '../../api/constants';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { PreferencesView } from '../../api/types';

const RECENT_KEY = 'notification-admin.recent-recipients';

function readRecent(): string[] {
    try {
        return JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]');
    } catch {
        return [];
    }
}

function rememberRecipient(id: string): string[] {
    const recent = [id, ...readRecent().filter(item => item !== id)].slice(0, 8);
    try {
        localStorage.setItem(RECENT_KEY, JSON.stringify(recent));
    } catch {
        // storage unavailable — recent list is a convenience only
    }
    return recent;
}

/** "22:00:00" / "22:00" / Date → "HH:mm" for the API (LocalTime). */
export function toLocalTime(value: unknown): string | null {
    if (value === null || value === undefined || value === '') return null;
    if (value instanceof Date) {
        return `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
    }
    const match = /^(\d{1,2}):(\d{2})/.exec(String(value));
    return match ? `${match[1].padStart(2, '0')}:${match[2]}` : null;
}

const timeZones = (() => {
    try {
        return (Intl as unknown as { supportedValuesOf(key: string): string[] }).supportedValuesOf('timeZone');
    } catch {
        return ['UTC', 'Asia/Almaty', 'Asia/Aqtobe', 'Europe/Moscow'];
    }
})();

const PreferencesForm = ({ recipientId }: { recipientId: string }) => {
    const translate = useTranslate();
    const notify = useNotify();
    const api = useNotificationApi();
    const queryClient = useQueryClient();
    const { data, isPending, error, refetch } = useGetOne<PreferencesView & { id: string }>('preferences', { id: recipientId });
    const [update, { isPending: saving }] = useUpdate();

    const defaults = useMemo(
        () =>
            data && {
                unsubscribedChannels: data.unsubscribedChannels ?? [],
                unsubscribedCategories: data.unsubscribedCategories ?? [],
                quietHoursStart: toLocalTime(data.quietHoursStart) ?? '',
                quietHoursEnd: toLocalTime(data.quietHoursEnd) ?? '',
                timeZone: data.timeZone ?? '',
                locale: data.locale ?? '',
            },
        [data],
    );

    if (isPending) return <CircularProgress />;
    if (error || !data || !defaults) return <Alert severity="error">{String((error as Error)?.message ?? '')}</Alert>;

    const patch = async (body: Parameters<typeof api.patchPreferences>[1], message: string) => {
        try {
            await api.patchPreferences(recipientId, body);
            notify(message, { type: 'success' });
            await queryClient.invalidateQueries({ queryKey: ['preferences'] });
            refetch();
        } catch (patchError) {
            notify((patchError as Error).message, { type: 'error' });
        }
    };

    const submit = (values: Record<string, unknown>) => {
        const start = toLocalTime(values.quietHoursStart);
        const end = toLocalTime(values.quietHoursEnd);
        if (Boolean(start) !== Boolean(end)) {
            notify('app.validation.quiet_hours_pair', { type: 'error' });
            return;
        }
        update(
            'preferences',
            {
                id: recipientId,
                data: { ...values, quietHoursStart: start, quietHoursEnd: end },
                previousData: data,
            },
            {
                onSuccess: () => notify('app.message.preferences_saved', { type: 'success' }),
                onError: updateError => notify((updateError as Error).message, { type: 'error' }),
            },
        );
    };

    return (
        <Card variant="outlined">
            <CardContent>
                <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                    <Typography variant="h6">{recipientId}</Typography>
                    {data.updatedAt ? (
                        <Chip size="small" label={`${translate('app.field.updatedAt')}: ${new Date(data.updatedAt).toLocaleString()}`} />
                    ) : (
                        <Chip size="small" color="info" label={translate('app.message.preferences_default')} />
                    )}
                </Stack>
                <Form key={JSON.stringify(defaults)} defaultValues={defaults} onSubmit={submit}>
                    <Grid container spacing={2}>
                        <Grid size={{ xs: 12, md: 6 }}>
                            <CheckboxGroupInput
                                source="unsubscribedChannels"
                                label="app.field.unsubscribedChannels"
                                choices={CHANNELS.map(id => ({ id, name: id }))}
                                helperText="app.help.unsubscribed_channels"
                            />
                        </Grid>
                        <Grid size={{ xs: 12, md: 6 }}>
                            <CheckboxGroupInput
                                source="unsubscribedCategories"
                                label="app.field.unsubscribedCategories"
                                choices={CATEGORIES.map(id => ({
                                    id,
                                    name: `app.category.${id}`,
                                    disabled: !OPTIONAL_CATEGORIES.includes(id),
                                }))}
                                helperText="app.help.unsubscribed_categories"
                            />
                        </Grid>
                        <Grid size={{ xs: 6, md: 3 }}>
                            <TimeInput source="quietHoursStart" label="app.field.quietHoursStart" format={value => toLocalTime(value) ?? ''} parse={value => value} />
                        </Grid>
                        <Grid size={{ xs: 6, md: 3 }}>
                            <TimeInput source="quietHoursEnd" label="app.field.quietHoursEnd" format={value => toLocalTime(value) ?? ''} parse={value => value} />
                        </Grid>
                        <Grid size={{ xs: 12, md: 3 }}>
                            <AutocompleteInput
                                source="timeZone"
                                label="app.field.timeZone"
                                choices={timeZones.map(id => ({ id, name: id }))}
                                helperText="app.help.time_zone"
                            />
                        </Grid>
                        <Grid size={{ xs: 12, md: 3 }}>
                            <TextInput source="locale" label="app.field.locale" helperText="app.help.recipient_locale" />
                        </Grid>
                    </Grid>
                    <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                        {translate('app.help.quiet_hours')}
                    </Typography>
                    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                        <SaveButton label="app.action.save_replace" disabled={saving} alwaysEnable />
                        <Button
                            variant="outlined"
                            disabled={!data.quietHoursStart}
                            onClick={() => patch({ clearQuietHours: true }, 'app.message.quiet_hours_cleared')}
                        >
                            {translate('app.action.clear_quiet_hours')}
                        </Button>
                        <Button variant="outlined" disabled={!data.locale} onClick={() => patch({ clearLocale: true }, 'app.message.locale_cleared')}>
                            {translate('app.action.clear_locale')}
                        </Button>
                    </Stack>
                </Form>
            </CardContent>
        </Card>
    );
};

export const PreferencesPage = () => {
    const translate = useTranslate();
    const [params, setParams] = useSearchParams();
    const recipientId = params.get('recipientId') ?? '';
    const [input, setInput] = useState(recipientId);
    const [recent, setRecent] = useState<string[]>(readRecent);

    useEffect(() => {
        setInput(recipientId);
        if (recipientId) setRecent(rememberRecipient(recipientId));
    }, [recipientId]);

    const open = (id: string) => {
        const trimmed = id.trim();
        if (trimmed) setParams({ recipientId: trimmed });
    };

    return (
        <Box p={2}>
            <Title title={translate('app.menu.preferences')} />
            <Card variant="outlined" sx={{ mb: 2 }}>
                <CardContent>
                    <Typography variant="body2" color="text.secondary" mb={2}>
                        {translate('app.help.preferences')}
                    </Typography>
                    <Stack direction="row" spacing={1} alignItems="center">
                        <TextField
                            size="small"
                            label={translate('app.field.recipientId')}
                            value={input}
                            onChange={event => setInput(event.target.value)}
                            onKeyDown={event => event.key === 'Enter' && open(input)}
                            sx={{ minWidth: 320 }}
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon fontSize="small" />
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />
                        <Button variant="contained" onClick={() => open(input)}>
                            {translate('app.action.open')}
                        </Button>
                    </Stack>
                    {recent.length > 0 && (
                        <Stack direction="row" spacing={0.5} mt={1} useFlexGap flexWrap="wrap" alignItems="center">
                            <Typography variant="caption" color="text.secondary" mr={1}>
                                {translate('app.help.recent')}
                            </Typography>
                            {recent.map(id => (
                                <Chip key={id} size="small" label={id} onClick={() => open(id)} color={id === recipientId ? 'primary' : 'default'} />
                            ))}
                        </Stack>
                    )}
                </CardContent>
            </Card>
            {recipientId && <PreferencesForm key={recipientId} recipientId={recipientId} />}
        </Box>
    );
};
