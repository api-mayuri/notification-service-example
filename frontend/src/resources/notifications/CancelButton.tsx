import { useState } from 'react';
import { Button, Confirm, useListContext, useNotify, useRecordContext, useRefresh, useTranslate, useUnselectAll } from 'react-admin';
import BlockIcon from '@mui/icons-material/Block';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { NotificationView } from '../../api/types';

/** Cancel is allowed only while no delivery has started (status ACCEPTED). */
export const CancelButton = () => {
    const record = useRecordContext<NotificationView>();
    const api = useNotificationApi();
    const notify = useNotify();
    const refresh = useRefresh();
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    if (!record) return null;
    const cancel = async () => {
        setLoading(true);
        try {
            await api.cancelNotification(record.id);
            notify('app.message.cancelled', { type: 'success' });
            refresh();
        } catch (error) {
            notify((error as Error).message, { type: 'error' });
        } finally {
            setLoading(false);
            setOpen(false);
        }
    };
    return (
        <>
            <Button
                label="app.action.cancel_notification"
                color="error"
                disabled={record.status !== 'ACCEPTED' || loading}
                onClick={() => setOpen(true)}
            >
                <BlockIcon />
            </Button>
            <Confirm
                isOpen={open}
                loading={loading}
                title="app.action.cancel_notification"
                content="app.message.cancel_confirm"
                onConfirm={cancel}
                onClose={() => setOpen(false)}
            />
        </>
    );
};

export const BulkCancelButton = () => {
    const { selectedIds, resource } = useListContext();
    const api = useNotificationApi();
    const notify = useNotify();
    const refresh = useRefresh();
    const unselectAll = useUnselectAll(resource ?? 'notifications');
    const translate = useTranslate();
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const cancel = async () => {
        setLoading(true);
        const results = await Promise.allSettled(selectedIds.map(id => api.cancelNotification(String(id))));
        const failed = results.filter(result => result.status === 'rejected').length;
        notify(translate('app.message.bulk_cancelled', { ok: results.length - failed, failed }), {
            type: failed ? 'warning' : 'success',
        });
        setLoading(false);
        setOpen(false);
        unselectAll();
        refresh();
    };
    return (
        <>
            <Button label="app.action.cancel_notification" color="error" onClick={() => setOpen(true)}>
                <BlockIcon />
            </Button>
            <Confirm
                isOpen={open}
                loading={loading}
                title="app.action.cancel_notification"
                content="app.message.cancel_confirm"
                onConfirm={cancel}
                onClose={() => setOpen(false)}
            />
        </>
    );
};
