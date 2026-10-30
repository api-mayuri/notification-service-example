import { useState } from 'react';
import { Title, useNotify, useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    FormControl,
    Grid,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import { Link } from 'react-router-dom';
import { CATEGORIES, CHANNEL_CONTACT, CHANNELS, LIMITS } from '../../api/constants';
import { newIdempotencyKey } from '../../api/dataProvider';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { BatchResponse, Category, NotificationRequest } from '../../api/types';
import { shortId } from '../../components/ShortIdField';

const EXAMPLE: NotificationRequest[] = [
    {
        recipient: { id: 'user-1', contacts: { email: 'user1@example.com' } },
        template: { key: 'welcome', locale: 'ru', variables: { name: 'Анна' } },
        channels: ['EMAIL'],
        category: 'TRANSACTIONAL',
    },
    {
        recipient: { id: 'user-2', contacts: { email: 'user2@example.com', phone: '+77011234567' } },
        template: { key: 'order-shipped', variables: { orderId: 'ORD-2' } },
        channels: ['EMAIL', 'SMS'],
        category: 'TRANSACTIONAL',
        idempotencyKey: 'order-2-shipped',
    },
];

/**
 * Recipients as CSV lines "id,contact[,var=value;var=value]" → batch items for one template.
 * The contact goes to the contact type of every selected channel.
 */
export function itemsFromRecipients(
    lines: string,
    options: { templateKey: string; locale?: string; channels: string[]; category: Category },
): NotificationRequest[] {
    return lines
        .split('\n')
        .map(line => line.trim())
        .filter(line => line && !line.startsWith('#'))
        .map(line => {
            const [id, contact, vars] = line.split(',').map(part => part?.trim());
            const contacts: Record<string, string> = {};
            if (contact) {
                for (const channel of options.channels) {
                    const type = CHANNEL_CONTACT[channel];
                    if (type) contacts[type] = contact;
                }
            }
            const variables: Record<string, string> = {};
            for (const pair of (vars ?? '').split(';')) {
                const [name, ...value] = pair.split('=');
                if (name?.trim()) variables[name.trim()] = value.join('=').trim();
            }
            return {
                recipient: { id, ...(Object.keys(contacts).length ? { contacts } : {}) },
                template: {
                    key: options.templateKey,
                    ...(options.locale ? { locale: options.locale } : {}),
                    ...(Object.keys(variables).length ? { variables } : {}),
                },
                channels: options.channels,
                category: options.category,
            };
        });
}

const statusColor = (status: number) => (status === 202 ? 'success' : status >= 500 ? 'error' : 'warning');

export const BatchSend = () => {
    const translate = useTranslate();
    const notify = useNotify();
    const api = useNotificationApi();
    const [tab, setTab] = useState(0);
    const [key, setKey] = useState(newIdempotencyKey());
    const [json, setJson] = useState(JSON.stringify(EXAMPLE, null, 2));
    const [recipients, setRecipients] = useState('user-1,anna@example.com,name=Анна\nuser-2,boris@example.com,name=Борис');
    const [templateKey, setTemplateKey] = useState('welcome');
    const [locale, setLocale] = useState('');
    const [channel, setChannel] = useState('EMAIL');
    const [category, setCategory] = useState<Category>('TRANSACTIONAL');
    const [result, setResult] = useState<BatchResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const parseItems = (): NotificationRequest[] => {
        if (tab === 1) {
            return itemsFromRecipients(recipients, { templateKey, locale, channels: [channel], category });
        }
        const parsed = JSON.parse(json);
        if (!Array.isArray(parsed)) throw new Error(translate('app.validation.batch_array'));
        return parsed;
    };

    const submit = async () => {
        setError(null);
        let items: NotificationRequest[];
        try {
            items = parseItems();
        } catch (parseError) {
            setError((parseError as Error).message);
            return;
        }
        if (!items.length || items.length > LIMITS.batchSize) {
            setError(translate('app.validation.batch_size', { max: LIMITS.batchSize }));
            return;
        }
        setLoading(true);
        try {
            const response = await api.sendBatch(items, key);
            setResult(response);
            notify('app.message.batch_done', { messageArgs: { accepted: response.accepted, rejected: response.rejected } });
        } catch (sendError) {
            setError((sendError as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ p: 2 }}>
            <Title title={translate('app.page.batch')} />
            <Card variant="outlined">
                <CardContent>
                    <Typography sx={{ mb: 2 }} variant="body2" color="text.secondary">
                        {translate('app.help.batch', { max: LIMITS.batchSize })}
                    </Typography>
                    <Stack sx={{ alignItems: 'center', mb: 2 }} direction="row" spacing={1}>
                        <TextField
                            label={translate('app.field.idempotencyKey')}
                            value={key}
                            onChange={event => setKey(event.target.value)}
                            size="small"
                            sx={{ minWidth: 380 }}
                        />
                        <Button size="small" onClick={() => setKey(newIdempotencyKey())}>
                            {translate('app.action.regenerate')}
                        </Button>
                    </Stack>
                    <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
                        <Tab label={translate('app.section.batch_json')} />
                        <Tab label={translate('app.section.batch_recipients')} />
                    </Tabs>
                    {tab === 0 ? (
                        <TextField
                            value={json}
                            onChange={event => setJson(event.target.value)}
                            multiline
                            minRows={14}
                            fullWidth
                            slotProps={{ input: { sx: { fontFamily: 'monospace', fontSize: 13 } } }}
                        />
                    ) : (
                        <Grid container spacing={2}>
                            <Grid size={{ xs: 12, md: 3 }}>
                                <TextField label={translate('app.field.templateKey')} value={templateKey} onChange={e => setTemplateKey(e.target.value)} fullWidth size="small" />
                            </Grid>
                            <Grid size={{ xs: 12, md: 3 }}>
                                <TextField label={translate('app.field.locale')} value={locale} onChange={e => setLocale(e.target.value)} fullWidth size="small" />
                            </Grid>
                            <Grid size={{ xs: 12, md: 3 }}>
                                <FormControl fullWidth size="small">
                                    <InputLabel>{translate('app.field.channel')}</InputLabel>
                                    <Select label={translate('app.field.channel')} value={channel} onChange={e => setChannel(e.target.value)}>
                                        {CHANNELS.map(id => (
                                            <MenuItem key={id} value={id}>
                                                {id}
                                            </MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                            </Grid>
                            <Grid size={{ xs: 12, md: 3 }}>
                                <FormControl fullWidth size="small">
                                    <InputLabel>{translate('app.field.category')}</InputLabel>
                                    <Select label={translate('app.field.category')} value={category} onChange={e => setCategory(e.target.value as Category)}>
                                        {CATEGORIES.map(id => (
                                            <MenuItem key={id} value={id}>
                                                {translate(`app.category.${id}`)}
                                            </MenuItem>
                                        ))}
                                    </Select>
                                </FormControl>
                            </Grid>
                            <Grid size={12}>
                                <TextField
                                    label={translate('app.field.recipients')}
                                    helperText={translate('app.help.batch_recipients')}
                                    value={recipients}
                                    onChange={event => setRecipients(event.target.value)}
                                    multiline
                                    minRows={8}
                                    fullWidth
                                    slotProps={{ input: { sx: { fontFamily: 'monospace', fontSize: 13 } } }}
                                />
                            </Grid>
                        </Grid>
                    )}
                    {error && (
                        <Alert severity="error" sx={{ mt: 2 }}>
                            {translate(error, { _: error })}
                        </Alert>
                    )}
                    <Box sx={{ mt: 2 }}>
                        <Button variant="contained" startIcon={<SendIcon />} onClick={submit} disabled={loading || !key.trim()}>
                            {translate('app.action.send_batch')}
                        </Button>
                    </Box>
                </CardContent>
            </Card>
            {result && (
                <Card variant="outlined" sx={{ mt: 2 }}>
                    <CardContent>
                        <Stack sx={{ mb: 2 }} direction="row" spacing={1}>
                            <Chip color="success" label={`${translate('app.field.accepted')}: ${result.accepted}`} />
                            <Chip color={result.rejected ? 'warning' : 'default'} label={`${translate('app.field.rejected')}: ${result.rejected}`} />
                        </Stack>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>#</TableCell>
                                    <TableCell>HTTP</TableCell>
                                    <TableCell>{translate('app.field.id')}</TableCell>
                                    <TableCell>{translate('app.field.status')}</TableCell>
                                    <TableCell>{translate('app.field.outcome')}</TableCell>
                                    <TableCell>{translate('app.field.error')}</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {result.items.map(item => (
                                    <TableRow key={item.index}>
                                        <TableCell>{item.index}</TableCell>
                                        <TableCell>
                                            <Chip size="small" color={statusColor(item.status)} label={item.status} />
                                        </TableCell>
                                        <TableCell>
                                            {item.notificationId ? (
                                                <Link to={`/notifications/${item.notificationId}/show`}>{shortId(item.notificationId)}</Link>
                                            ) : (
                                                '—'
                                            )}
                                        </TableCell>
                                        <TableCell>{item.notificationStatus ?? '—'}</TableCell>
                                        <TableCell>{item.outcome ?? '—'}</TableCell>
                                        <TableCell>{item.errorCode ? `${item.errorCode}: ${item.detail ?? ''}` : '—'}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>
            )}
        </Box>
    );
};
