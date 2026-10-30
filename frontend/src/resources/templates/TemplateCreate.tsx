import { BooleanInput, Create, SelectInput, SimpleForm, TextInput, required, useTranslate } from 'react-admin';
import { useWatch } from 'react-hook-form';
import { Stack } from '@mui/material';
import { CHANNELS } from '../../api/constants';
import type { CreateTemplateRequest } from '../../api/types';
import { normalizeDuration, validateDuration } from '../../utils/duration';
import { TemplateContentInputs } from './TemplateContentInputs';

const ContentForChannel = () => {
    const channel = useWatch({ name: 'channel' });
    return <TemplateContentInputs channel={channel} />;
};

// Same rule as TemplateKey on the backend: kebab-case, up to 100 characters.
const keyPattern = (value: string) =>
    value && (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(value.trim()) || value.trim().length > 100) ? 'app.validation.template_key' : undefined;

export const TemplateCreate = () => {
    const translate = useTranslate();
    return (
        <Create
            title={translate('app.page.template_create')}
            redirect="show"
            transform={(values: CreateTemplateRequest & { deduplicationWindow?: string }) => ({
                ...values,
                key: values.key.trim(),
                locale: values.locale.trim(),
                subject: values.subject || null,
                textBody: values.textBody || null,
                requiredVariables: (values.requiredVariables ?? []).filter(Boolean),
                deduplicationWindow: normalizeDuration(values.deduplicationWindow),
            })}
        >
            <SimpleForm defaultValues={{ channel: 'EMAIL', locale: 'ru', activate: true, requiredVariables: [] }}>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} width="100%">
                    <TextInput source="key" label="app.field.key" validate={[required(), keyPattern]} helperText="app.help.template_key" />
                    <SelectInput source="channel" label="app.field.channel" choices={CHANNELS.map(id => ({ id, name: id }))} validate={required()} />
                    <TextInput source="locale" label="app.field.locale" validate={required()} helperText="app.help.template_locale" />
                    <TextInput
                        source="deduplicationWindow"
                        label="app.field.deduplicationWindow"
                        validate={validateDuration}
                        helperText="app.help.deduplication_window"
                    />
                </Stack>
                <ContentForChannel />
                <BooleanInput source="activate" label="app.field.activate" helperText="app.help.activate" />
            </SimpleForm>
        </Create>
    );
};
