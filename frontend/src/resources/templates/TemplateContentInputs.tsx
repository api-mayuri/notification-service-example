import { TextArrayInput, TextInput, useTranslate } from 'react-admin';
import { useFormContext } from 'react-hook-form';
import { Alert, Box, Button } from '@mui/material';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import StyleIcon from '@mui/icons-material/Style';
import { STYLE_SNIPPET } from '../../templates/render';

/** Variable names used in a Thymeleaf template: ${name}, ${name.field}, [(${name})]. */
export function detectVariables(...sources: (string | null | undefined)[]): string[] {
    const names = new Set<string>();
    for (const source of sources) {
        for (const match of (source ?? '').matchAll(/\$\{\s*([A-Za-z_][\w]*)/g)) {
            names.add(match[1]);
        }
    }
    return [...names].sort();
}

const DetectVariablesButton = () => {
    const { getValues, setValue } = useFormContext();
    const translate = useTranslate();
    return (
        <Button
            size="small"
            startIcon={<AutoFixHighIcon />}
            onClick={() => {
                const [subject, body, textBody] = getValues(['subject', 'body', 'textBody']);
                setValue('requiredVariables', detectVariables(subject, body, textBody), { shouldDirty: true });
            }}
        >
            {translate('app.action.detect_variables')}
        </Button>
    );
};

/** Prepends a <style> skeleton to the HTML body (HTML + CSS templates). */
const InsertStyleButton = () => {
    const { getValues, setValue } = useFormContext();
    const translate = useTranslate();
    return (
        <Button
            size="small"
            startIcon={<StyleIcon />}
            onClick={() => setValue('body', STYLE_SNIPPET + (getValues('body') ?? ''), { shouldDirty: true })}
        >
            {translate('app.action.insert_style')}
        </Button>
    );
};

/** Subject, body, text body and required variables: shared by "create" and "add version". */
export const TemplateContentInputs = ({ channel }: { channel?: string }) => {
    const translate = useTranslate();
    const isEmail = !channel || channel === 'EMAIL';
    return (
        <Box sx={{ width: '100%' }}>
            <Alert severity="info" sx={{ mb: 2 }}>
                {translate(isEmail ? 'app.help.template_syntax_email' : 'app.help.template_syntax_text')}
            </Alert>
            <TextInput source="subject" label="app.field.subject" fullWidth helperText={isEmail ? 'app.help.subject' : 'app.help.subject_optional'} />
            <TextInput
                source="body"
                label={isEmail ? 'app.field.html_body' : 'app.field.body'}
                multiline
                minRows={6}
                fullWidth
                validate={value => (value?.trim() ? undefined : 'ra.validation.required')}
                sx={{ '& textarea': { fontFamily: 'monospace', fontSize: 13 } }}
            />
            {isEmail && (
                <TextInput
                    source="textBody"
                    label="app.field.text_body"
                    multiline
                    minRows={3}
                    fullWidth
                    helperText="app.help.text_body"
                    sx={{ '& textarea': { fontFamily: 'monospace', fontSize: 13 } }}
                />
            )}
            <TextArrayInput source="requiredVariables" label="app.field.requiredVariables" helperText="app.help.required_variables" fullWidth />
            <DetectVariablesButton />
            {isEmail && <InsertStyleButton />}
        </Box>
    );
};
