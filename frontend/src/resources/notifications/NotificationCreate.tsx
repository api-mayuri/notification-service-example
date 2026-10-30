import { useEffect, useMemo, useState } from 'react';
import {
    CheckboxGroupInput,
    Create,
    DateTimeInput,
    RadioButtonGroupInput,
    SaveButton,
    SelectInput,
    SimpleForm,
    TextInput,
    Toolbar,
    required,
    useGetList,
    useNotify,
    usePermissions,
    useRedirect,
    useTranslate,
    type Validator,
} from 'react-admin';
import { useFormContext, useWatch } from 'react-hook-form';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    Divider,
    IconButton,
    InputAdornment,
    Stack,
    Tab,
    Tabs,
    Tooltip,
    Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import RefreshIcon from '@mui/icons-material/Autorenew';
import { config } from '../../config';
import { CATEGORIES, CHANNEL_CONTACT, CHANNELS, CONTACT_TYPES, LIMITS, PRIORITIES, SCOPES } from '../../api/constants';
import { newIdempotencyKey } from '../../api/dataProvider';
import type { AcceptedResult } from '../../api/dataProvider';
import type { TemplateView } from '../../api/types';
import { hasScope } from '../../auth/authProvider';
import { KeyValueInput, type Entry } from '../../components/KeyValueInput';
import { CodeBlock } from '../../components/CodeBlock';
import { buildNotificationRequest, toCurl, toKafkaCommand, type NotificationFormValues } from '../../utils/requestBuilder';

const IdempotencyKeyInput = () => {
    const { setValue } = useFormContext();
    const translate = useTranslate();
    return (
        <TextInput
            source="idempotencyKey"
            label="app.field.idempotencyKey"
            validate={required()}
            helperText="app.help.idempotency_key"
            fullWidth
            slotProps={{
                input: {
                    endAdornment: (
                        <InputAdornment position="end">
                            <Tooltip title={translate('app.action.regenerate')}>
                                <IconButton size="small" onClick={() => setValue('idempotencyKey', newIdempotencyKey(), { shouldDirty: true })}>
                                    <RefreshIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>
                        </InputAdornment>
                    ),
                },
            }}
        />
    );
};

/** Template keys known to the backend (needs templates:manage) with their required variables. */
const TemplateHints = () => {
    const { permissions } = usePermissions();
    const canList = hasScope(permissions, SCOPES.templates);
    const { data } = useGetList<TemplateView>(
        'templates',
        { pagination: { page: 1, perPage: 1000 }, sort: { field: 'key', order: 'ASC' } },
        { enabled: canList },
    );
    const { setValue, getValues } = useFormContext();
    const translate = useTranslate();
    const key = useWatch({ name: 'templateKey' });
    const channels: string[] = useWatch({ name: 'channels' }) ?? [];

    const keys = useMemo(() => [...new Set((data ?? []).map(t => t.key))], [data]);
    const variants = useMemo(() => (data ?? []).filter(t => t.key === key), [data, key]);
    const required = useMemo(() => {
        const names = new Set<string>();
        for (const variant of variants) {
            const active = variant.versions.find(v => v.version === variant.activeVersion);
            active?.requiredVariables.forEach(name => names.add(name));
        }
        return [...names];
    }, [variants]);

    // Add rows for required variables the form does not have yet.
    useEffect(() => {
        if (!required.length) return;
        const current: Entry[] = getValues('variables') ?? [];
        const missing = required.filter(name => !current.some(entry => entry?.key === name));
        if (missing.length) {
            setValue('variables', [...current.filter(entry => entry?.key), ...missing.map(name => ({ key: name, value: '' }))], {
                shouldDirty: true,
            });
        }
    }, [required, getValues, setValue]);

    if (!canList) return null;
    const missingChannels = channels.filter(channel => variants.length && !variants.some(v => v.channel === channel));
    return (
        <Box mb={1}>
            <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap" alignItems="center">
                <Typography variant="caption" color="text.secondary" mr={1}>
                    {translate('app.help.available_templates')}
                </Typography>
                {keys.map(item => (
                    <Chip
                        key={item}
                        size="small"
                        label={item}
                        color={item === key ? 'primary' : 'default'}
                        onClick={() => setValue('templateKey', item, { shouldDirty: true })}
                    />
                ))}
            </Stack>
            {variants.length > 0 && (
                <Typography variant="caption" color="text.secondary" display="block" mt={1}>
                    {translate('app.help.template_variants', {
                        variants: variants.map(v => `${v.channel}/${v.locale}${v.activeVersion ? '' : ' (draft)'}`).join(', '),
                    })}
                </Typography>
            )}
            {missingChannels.length > 0 && (
                <Alert severity="warning" sx={{ mt: 1 }}>
                    {translate('app.help.template_missing_channels', { channels: missingChannels.join(', ') })}
                </Alert>
            )}
        </Box>
    );
};

const ContactInputs = () => {
    const translate = useTranslate();
    const channels: string[] = useWatch({ name: 'channels' }) ?? [];
    const needed = new Set(channels.map(channel => CHANNEL_CONTACT[channel]).filter(Boolean));
    return (
        <Box width="100%">
            <Typography variant="subtitle2" gutterBottom>
                {translate('app.section.contacts')}
            </Typography>
            <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                {translate('app.help.contacts')}
            </Typography>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} useFlexGap flexWrap="wrap">
                {CONTACT_TYPES.map(type => (
                    <TextInput
                        key={type}
                        source={`contacts.${type}`}
                        label={`app.contact.${type}`}
                        helperText={needed.has(type) ? 'app.help.contact_needed' : false}
                        sx={{ minWidth: 220 }}
                    />
                ))}
            </Stack>
            <KeyValueInput source="extraContacts" label="app.field.extraContacts" keyLabel="app.field.contactType" valueLabel="app.field.value" />
        </Box>
    );
};

const RequestPreview = () => {
    const values = useWatch() as NotificationFormValues;
    const translate = useTranslate();
    const [tab, setTab] = useState(0);
    const request = buildNotificationRequest(values);
    const key = values.idempotencyKey ?? '';
    const code =
        tab === 0
            ? JSON.stringify(request, null, 2)
            : tab === 1
              ? toCurl(request, key, config.apiUrl)
              : toKafkaCommand(request, key, 'urn:local:demo-producer');
    return (
        <Accordion variant="outlined" sx={{ width: '100%', mt: 2 }} disableGutters>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="subtitle2">{translate('app.section.request_preview')}</Typography>
            </AccordionSummary>
            <AccordionDetails>
                <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 1 }}>
                    <Tab label="JSON" />
                    <Tab label="curl" />
                    <Tab label="Kafka (CloudEvents)" />
                </Tabs>
                {tab === 2 && (
                    <Alert severity="info" sx={{ mb: 1 }}>
                        {translate('app.help.kafka_command')}
                    </Alert>
                )}
                <CodeBlock code={code} />
            </AccordionDetails>
        </Accordion>
    );
};

const ContentInputs = () => {
    const mode = useWatch({ name: 'mode' });
    const { permissions } = usePermissions();
    const translate = useTranslate();
    if (mode === 'content') {
        return (
            <Box width="100%">
                {!hasScope(permissions, SCOPES.sendRaw) && (
                    <Alert severity="warning" sx={{ mb: 1 }}>
                        {translate('app.help.raw_scope_missing')}
                    </Alert>
                )}
                <TextInput source="subject" label="app.field.subject" fullWidth />
                <TextInput source="text" label="app.field.text" multiline minRows={3} fullWidth />
                <TextInput source="html" label="app.field.html" multiline minRows={4} fullWidth />
            </Box>
        );
    }
    return (
        <Box width="100%">
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextInput source="templateKey" label="app.field.templateKey" validate={required()} sx={{ minWidth: 260 }} />
                <TextInput source="templateLocale" label="app.field.locale" helperText="app.help.locale" sx={{ minWidth: 200 }} />
            </Stack>
            <TemplateHints />
            <KeyValueInput source="variables" label="app.field.variables" keyLabel="app.field.variable" helperText="app.help.variables" />
        </Box>
    );
};

const validateForm = (values: NotificationFormValues) => {
    const errors: Record<string, string> = {};
    if (!values.channels?.length) errors.channels = 'ra.validation.required';
    if ((values.channels?.length ?? 0) > LIMITS.channelsPerRequest) errors.channels = 'app.validation.too_many_channels';
    if (values.mode === 'content' && !values.text?.trim() && !values.html?.trim()) errors.text = 'app.validation.content_required';
    if ((values.metadata?.length ?? 0) > LIMITS.metadataEntries) errors.metadata = 'app.validation.too_many_metadata';
    return errors;
};

const FormToolbar = () => (
    <Toolbar>
        <SaveButton label="app.action.send" alwaysEnable />
    </Toolbar>
);

const channelValidator: Validator = value => (value?.length ? undefined : 'ra.validation.required');

export const NotificationCreate = () => {
    const notify = useNotify();
    const redirect = useRedirect();
    const translate = useTranslate();
    const defaults = useMemo<NotificationFormValues>(
        () => ({
            idempotencyKey: newIdempotencyKey(),
            mode: 'template',
            channels: ['EMAIL'],
            category: 'TRANSACTIONAL',
            priority: 'NORMAL',
            variables: [],
            metadata: [],
            extraContacts: [],
        }),
        [],
    );
    return (
        <Create
            title={translate('app.page.send')}
            transform={(values: NotificationFormValues) => ({
                ...buildNotificationRequest(values),
                idempotencyKey: values.idempotencyKey,
            })}
            mutationOptions={{
                onSuccess: (data: AcceptedResult) => {
                    const outcome = data.replayed ? 'REPLAYED' : data.outcome;
                    notify(`app.outcome.${outcome}`, { type: outcome === 'CREATED' ? 'success' : 'info', messageArgs: { id: data.id } });
                    redirect('show', 'notifications', data.id);
                },
            }}
        >
            <SimpleForm defaultValues={defaults} validate={validateForm} toolbar={<FormToolbar />}>
                <IdempotencyKeyInput />
                <Divider flexItem sx={{ my: 1 }} />
                <TextInput source="recipientId" label="app.field.recipientId" validate={required()} helperText="app.help.recipient_id" />
                <CheckboxGroupInput
                    source="channels"
                    label="app.field.channels"
                    choices={CHANNELS.map(id => ({ id, name: id }))}
                    validate={channelValidator}
                />
                <ContactInputs />
                <Divider flexItem sx={{ my: 1 }} />
                <RadioButtonGroupInput
                    source="mode"
                    label="app.field.content_mode"
                    choices={[
                        { id: 'template', name: 'app.field.mode_template' },
                        { id: 'content', name: 'app.field.mode_content' },
                    ]}
                />
                <ContentInputs />
                <Divider flexItem sx={{ my: 1 }} />
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                    <SelectInput
                        source="category"
                        label="app.field.category"
                        choices={CATEGORIES.map(id => ({ id, name: `app.category.${id}` }))}
                        validate={required()}
                        helperText="app.help.category"
                    />
                    <SelectInput
                        source="priority"
                        label="app.field.priority"
                        choices={PRIORITIES.map(id => ({ id, name: `app.priority.${id}` }))}
                        helperText="app.help.priority"
                    />
                    <DateTimeInput source="sendAt" label="app.field.sendAt" helperText="app.help.send_at" />
                </Stack>
                <KeyValueInput source="metadata" label="app.field.metadata" helperText="app.help.metadata" />
                <RequestPreview />
                <Box mt={1}>
                    <Button size="small" href={config.mailpitUrl} target="_blank" rel="noreferrer">
                        {translate('app.action.open_mailpit')}
                    </Button>
                </Box>
            </SimpleForm>
        </Create>
    );
};
