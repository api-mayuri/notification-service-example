import { useState } from 'react';
import { BooleanInput, Form, SaveButton, TextInput, useNotify, useRecordContext, useRefresh, useTranslate, useUpdate } from 'react-admin';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import TimerIcon from '@mui/icons-material/Timer';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { AddVersionRequest, TemplateVersion, TemplateView } from '../../api/types';
import { durationToSeconds, normalizeDuration, secondsToIso, validateDuration } from '../../utils/duration';
import { LiveTemplatePreview } from '../../templates/LiveTemplatePreview';
import { TemplateContentInputs } from './TemplateContentInputs';

/** POST /templates/{key}/versions: new DRAFT version, optionally activated at once. */
export const AddVersionDialog = ({
    open,
    onClose,
    base,
}: {
    open: boolean;
    onClose: () => void;
    base?: TemplateVersion | null;
}) => {
    const record = useRecordContext<TemplateView>();
    const api = useNotificationApi();
    const notify = useNotify();
    const refresh = useRefresh();
    const translate = useTranslate();
    if (!record) return null;
    const submit = async (values: Record<string, unknown>) => {
        const request: AddVersionRequest = {
            channel: record.channel,
            locale: record.locale,
            subject: (values.subject as string) || null,
            body: values.body as string,
            textBody: (values.textBody as string) || null,
            requiredVariables: ((values.requiredVariables as string[]) ?? []).filter(Boolean),
            activate: Boolean(values.activate),
        };
        try {
            await api.addTemplateVersion(record.key, request);
            notify('app.message.version_added', { type: 'success' });
            refresh();
            onClose();
        } catch (error) {
            notify((error as Error).message, { type: 'error' });
        }
    };
    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="xl">
            <Form
                onSubmit={submit}
                defaultValues={{
                    subject: base?.subject ?? '',
                    body: base?.body ?? '',
                    textBody: base?.textBody ?? '',
                    requiredVariables: base?.requiredVariables ?? [],
                    activate: false,
                }}
            >
                <DialogTitle>
                    {translate('app.action.add_version')} · {record.key} · {record.channel}/{record.locale}
                </DialogTitle>
                <DialogContent>
                    <Grid container spacing={3}>
                        <Grid size={{ xs: 12, lg: 6 }}>
                            <TemplateContentInputs channel={record.channel} />
                            <BooleanInput source="activate" label="app.field.activate" helperText="app.help.activate" />
                        </Grid>
                        <Grid size={{ xs: 12, lg: 6 }}>
                            <LiveTemplatePreview channel={record.channel} />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>{translate('ra.action.cancel')}</Button>
                    <SaveButton label="app.action.add_version" icon={<AddIcon />} />
                </DialogActions>
            </Form>
        </Dialog>
    );
};

/** PUT /templates/{key}: deduplication window (the only mutable template setting). */
export const SettingsDialog = ({ open, onClose }: { open: boolean; onClose: () => void }) => {
    const record = useRecordContext<TemplateView>();
    const [update] = useUpdate();
    const notify = useNotify();
    const translate = useTranslate();
    if (!record) return null;
    const seconds = durationToSeconds(record.deduplicationWindow);
    const submit = (values: Record<string, unknown>) =>
        update(
            'templates',
            {
                id: record.id,
                data: { deduplicationWindow: normalizeDuration(values.deduplicationWindow as string) },
                previousData: record,
            },
            {
                onSuccess: () => {
                    notify('app.message.settings_saved', { type: 'success' });
                    onClose();
                },
                onError: error => notify((error as Error).message, { type: 'error' }),
            },
        );
    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <Form onSubmit={submit} defaultValues={{ deduplicationWindow: seconds === null ? '' : secondsToIso(seconds) }}>
                <DialogTitle>{translate('app.action.template_settings')}</DialogTitle>
                <DialogContent>
                    <TextInput
                        source="deduplicationWindow"
                        label="app.field.deduplicationWindow"
                        validate={validateDuration}
                        helperText="app.help.deduplication_window"
                        fullWidth
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>{translate('ra.action.cancel')}</Button>
                    <SaveButton icon={<TimerIcon />} />
                </DialogActions>
            </Form>
        </Dialog>
    );
};

export const useDialog = () => {
    const [open, setOpen] = useState(false);
    return { open, show: () => setOpen(true), close: () => setOpen(false) };
};
