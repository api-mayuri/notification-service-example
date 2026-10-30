import {
    CreateButton,
    Datagrid,
    DateField,
    ExportButton,
    FunctionField,
    List,
    SearchInput,
    SelectInput,
    TextField,
    TextInput,
    TopToolbar,
    useTranslate,
} from 'react-admin';
import { CHANNELS } from '../../api/constants';
import type { TemplateView } from '../../api/types';
import { ChannelChip } from '../../components/ChannelChip';
import { StatusChip } from '../../components/StatusChip';
import { humanizeDuration } from '../../utils/duration';

const filters = [
    <SearchInput key="q" source="q" alwaysOn />,
    <SelectInput key="channel" source="channel" label="app.field.channel" choices={CHANNELS.map(id => ({ id, name: id }))} alwaysOn />,
    <TextInput key="locale" source="locale" label="app.field.locale" />,
];

const Actions = () => (
    <TopToolbar>
        <CreateButton />
        <ExportButton />
    </TopToolbar>
);

export const TemplateList = () => {
    const translate = useTranslate();
    return (
        <List filters={filters} actions={<Actions />} sort={{ field: 'key', order: 'ASC' }} perPage={25} title={translate('app.menu.templates')}>
            <Datagrid rowClick="show" bulkActionButtons={false}>
                <TextField source="key" label="app.field.key" />
                <FunctionField source="channel" label="app.field.channel" render={(t: TemplateView) => <ChannelChip channel={t.channel} />} />
                <TextField source="locale" label="app.field.locale" />
                <FunctionField
                    source="activeVersion"
                    label="app.field.activeVersion"
                    render={(t: TemplateView) => (t.activeVersion ? `v${t.activeVersion}` : <StatusChip status="DRAFT" />)}
                />
                <FunctionField label="app.field.versions" render={(t: TemplateView) => t.versions.length} />
                <FunctionField
                    source="deduplicationWindow"
                    label="app.field.deduplicationWindow"
                    render={(t: TemplateView) => humanizeDuration(t.deduplicationWindow)}
                />
                <DateField source="updatedAt" label="app.field.updatedAt" showTime />
            </Datagrid>
        </List>
    );
};
