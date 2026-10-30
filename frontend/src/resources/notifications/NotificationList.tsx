import {
    CreateButton,
    Datagrid,
    DateField,
    DateTimeInput,
    ExportButton,
    FilterButton,
    List,
    SelectInput,
    TextField,
    TextInput,
    TopToolbar,
    useListContext,
    usePermissions,
    useTranslate,
} from 'react-admin';
import { Button } from '@mui/material';
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd';
import { Link } from 'react-router-dom';
import { CHANNELS, NOTIFICATION_STATUSES, SCOPES } from '../../api/constants';
import { hasScope } from '../../auth/authProvider';
import { ChannelsField } from '../../components/ChannelChip';
import { StatusField } from '../../components/StatusChip';
import { ShortIdField } from '../../components/ShortIdField';
import { BulkCancelButton } from './CancelButton';

const choices = (values: string[], prefix: string, translate: (key: string, options?: object) => string) =>
    values.map(value => ({ id: value, name: translate(`${prefix}.${value}`, { _: value }) }));

const useNotificationFilters = () => {
    const translate = useTranslate();
    return [
        <TextInput key="recipientId" source="recipientId" label="app.field.recipientId" alwaysOn resettable />,
        <SelectInput key="status" source="status" label="app.field.status" choices={choices(NOTIFICATION_STATUSES, 'app.status', translate)} alwaysOn />,
        <SelectInput key="channel" source="channel" label="app.field.channel" choices={CHANNELS.map(id => ({ id, name: id }))} />,
        <DateTimeInput key="from" source="from" label="app.field.from" />,
        <DateTimeInput key="to" source="to" label="app.field.to" />,
    ];
};

const ListActions = () => {
    const { permissions } = usePermissions();
    const translate = useTranslate();
    const { total } = useListContext();
    const canSend = hasScope(permissions, SCOPES.send);
    return (
        <TopToolbar>
            <FilterButton />
            {canSend && <CreateButton label="app.action.send" />}
            {canSend && (
                <Button component={Link} to="/notifications/batch" size="small" startIcon={<PlaylistAddIcon />}>
                    {translate('app.action.batch')}
                </Button>
            )}
            <ExportButton disabled={!total} maxResults={200} />
        </TopToolbar>
    );
};

const BulkActions = () => {
    const { permissions } = usePermissions();
    return hasScope(permissions, SCOPES.send) ? <BulkCancelButton /> : null;
};

export const NotificationList = () => {
    const translate = useTranslate();
    return (
        <List
            filters={useNotificationFilters()}
            actions={<ListActions />}
            perPage={20}
            sort={{ field: 'createdAt', order: 'DESC' }}
            title={translate('app.menu.notifications')}
            empty={false}
        >
            <Datagrid rowClick="show" bulkActionButtons={<BulkActions />}>
                <ShortIdField source="id" label="app.field.id" sortable={false} />
                <DateField source="createdAt" label="app.field.createdAt" showTime sortable={false} />
                <TextField source="recipientId" label="app.field.recipientId" sortable={false} />
                <StatusField source="status" label="app.field.status" sortable={false} />
                <ChannelsField source="channels" label="app.field.channels" sortable={false} />
                <TextField source="category" label="app.field.category" sortable={false} />
                <TextField source="priority" label="app.field.priority" sortable={false} />
                <TextField source="templateKey" label="app.field.templateKey" sortable={false} emptyText="—" />
            </Datagrid>
        </List>
    );
};
