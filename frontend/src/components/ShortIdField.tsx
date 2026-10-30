import { Stack, Tooltip, Typography } from '@mui/material';
import { useRecordContext } from 'react-admin';
import { CopyButton } from './CopyButton';

export const shortId = (id: string) => (id.length > 13 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id);

export const ShortIdField = ({ source = 'id', copy = false }: { source?: string; label?: string; copy?: boolean; sortable?: boolean }) => {
    const record = useRecordContext();
    const value = record?.[source];
    if (!value) return null;
    return (
        <Stack sx={{ alignItems: 'center' }} direction="row" spacing={0.5}>
            <Tooltip title={String(value)}>
                <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                    {shortId(String(value))}
                </Typography>
            </Tooltip>
            {copy && <CopyButton text={String(value)} />}
        </Stack>
    );
};
