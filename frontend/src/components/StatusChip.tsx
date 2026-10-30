import { Chip, type ChipProps } from '@mui/material';
import { useRecordContext, useTranslate } from 'react-admin';
import { STATUS_COLORS } from '../api/constants';

export const StatusChip = ({ status, size = 'small', ...rest }: { status?: string | null } & ChipProps) => {
    const translate = useTranslate();
    if (!status) return null;
    return (
        <Chip
            size={size}
            color={STATUS_COLORS[status] ?? 'default'}
            label={translate(`app.status.${status}`, { _: status })}
            variant={status === 'CANCELLED' || status === 'SKIPPED' ? 'outlined' : 'filled'}
            {...rest}
        />
    );
};

/** Field version for Datagrid columns. */
export const StatusField = ({ source }: { source: string; label?: string; sortable?: boolean }) => {
    const record = useRecordContext();
    return <StatusChip status={record?.[source]} />;
};
