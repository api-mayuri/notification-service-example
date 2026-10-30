import { useEffect, useMemo, useState } from 'react';
import { useRecordContext, useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    FormControl,
    Grid,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { TemplatePreview, TemplateView } from '../../api/types';
import { parseVariableValue } from '../../components/KeyValueInput';
import { HtmlPreview } from '../../components/HtmlPreview';
import { CodeBlock } from '../../components/CodeBlock';

/** POST /templates/{key}/preview: render a version with test variables, nothing is sent. */
export const TemplatePreviewPanel = ({ initialVersion }: { initialVersion?: number | null }) => {
    const record = useRecordContext<TemplateView>();
    const api = useNotificationApi();
    const translate = useTranslate();
    const [version, setVersion] = useState<number | ''>(initialVersion ?? record?.activeVersion ?? '');
    const [variables, setVariables] = useState<Record<string, string>>({});
    const [extra, setExtra] = useState('');
    const [preview, setPreview] = useState<TemplatePreview | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (initialVersion) setVersion(initialVersion);
    }, [initialVersion]);

    const selected = useMemo(
        () => record?.versions.find(v => v.version === (version || record.activeVersion)) ?? record?.versions.at(-1),
        [record, version],
    );
    if (!record) return null;

    const run = async () => {
        setLoading(true);
        setError(null);
        try {
            let extraVariables: Record<string, unknown> = {};
            if (extra.trim()) extraVariables = JSON.parse(extra);
            const values: Record<string, unknown> = { ...extraVariables };
            for (const [name, value] of Object.entries(variables)) values[name] = parseVariableValue(value);
            setPreview(
                await api.previewTemplate(record.key, {
                    channel: record.channel,
                    locale: record.locale,
                    version: version || null,
                    variables: values,
                }),
            );
        } catch (previewError) {
            setPreview(null);
            setError((previewError as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box>
            <Grid container spacing={2}>
                <Grid size={{ xs: 12, md: 4 }}>
                    <FormControl fullWidth size="small">
                        <InputLabel>{translate('app.field.version')}</InputLabel>
                        <Select label={translate('app.field.version')} value={version} onChange={event => setVersion(event.target.value as number | '')}>
                            <MenuItem value="">{translate('app.field.active_version_default')}</MenuItem>
                            {record.versions.map(v => (
                                <MenuItem key={v.version} value={v.version}>
                                    v{v.version} · {translate(`app.status.${v.status}`)}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Grid>
                {(selected?.requiredVariables ?? []).map(name => (
                    <Grid key={name} size={{ xs: 12, md: 4 }}>
                        <TextField
                            size="small"
                            fullWidth
                            label={name}
                            value={variables[name] ?? ''}
                            onChange={event => setVariables({ ...variables, [name]: event.target.value })}
                        />
                    </Grid>
                ))}
                <Grid size={12}>
                    <TextField
                        size="small"
                        fullWidth
                        multiline
                        minRows={2}
                        label={translate('app.field.extra_variables_json')}
                        placeholder='{"items": [{"name": "Книга", "price": 1200}]}'
                        value={extra}
                        onChange={event => setExtra(event.target.value)}
                    />
                </Grid>
            </Grid>
            <Stack direction="row" mt={2}>
                <Button variant="contained" startIcon={<VisibilityIcon />} onClick={run} disabled={loading}>
                    {translate('app.action.preview')}
                </Button>
            </Stack>
            {error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {translate(error, { _: error })}
                </Alert>
            )}
            {preview && (
                <Box mt={2}>
                    <Typography variant="caption" color="text.secondary">
                        v{preview.version} · {preview.locale}
                    </Typography>
                    {preview.subject && (
                        <Typography variant="h6" gutterBottom>
                            {preview.subject}
                        </Typography>
                    )}
                    {preview.htmlBody && <HtmlPreview html={preview.htmlBody} />}
                    {preview.textBody && (
                        <Box mt={2}>
                            <Typography variant="subtitle2">{translate('app.field.text_body')}</Typography>
                            <CodeBlock code={preview.textBody} />
                        </Box>
                    )}
                </Box>
            )}
        </Box>
    );
};
