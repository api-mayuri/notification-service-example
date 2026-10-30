import { useDeferredValue, useMemo, useState } from 'react';
import { useTranslate } from 'react-admin';
import { useWatch } from 'react-hook-form';
import {
    Alert,
    Box,
    Card,
    CardContent,
    Chip,
    Grid,
    Stack,
    Tab,
    Tabs,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography,
} from '@mui/material';
import DesktopIcon from '@mui/icons-material/DesktopWindows';
import PhoneIcon from '@mui/icons-material/PhoneIphone';
import { parseVariableValue } from '../components/KeyValueInput';
import { detectVariables } from '../resources/templates/TemplateContentInputs';
import { detectFormat, renderPreviewDocument, substituteVariables, type PreviewFormat } from './render';

const WIDTHS = { desktop: '100%', mobile: '375px' } as const;

/** Sample value shown until the user types one: a readable marker instead of an empty gap. */
const sample = (name: string) => `‹${name}›`;

/**
 * Live preview next to the template editor: renders the form values in the browser with test
 * variables. Fields are read from the surrounding react-hook-form (subject, body, textBody).
 */
export const LiveTemplatePreview = ({ channel }: { channel?: string }) => {
    const translate = useTranslate();
    const [subject, body, textBody, required] = useWatch({ name: ['subject', 'body', 'textBody', 'requiredVariables'] });
    // Typing stays responsive: the iframe re-renders with a deferred value.
    const deferred = useDeferredValue({ subject: subject ?? '', body: body ?? '', textBody: textBody ?? '' });
    const [format, setFormat] = useState<'auto' | PreviewFormat>('auto');
    const [width, setWidth] = useState<keyof typeof WIDTHS>('desktop');
    const [tab, setTab] = useState(0);
    const [values, setValues] = useState<Record<string, string>>({});

    const names = useMemo(
        () => [...new Set([...(required ?? []).filter(Boolean), ...detectVariables(deferred.subject, deferred.body, deferred.textBody)])],
        [required, deferred],
    );
    const variables = useMemo(() => {
        const result: Record<string, unknown> = {};
        for (const name of names) result[name] = values[name] ? parseVariableValue(values[name]) : sample(name);
        return result;
    }, [names, values]);

    const detected = detectFormat(deferred.body);
    const effective = format === 'auto' ? detected : format;
    const source = tab === 0 ? deferred.body : deferred.textBody;
    const document = useMemo(
        () => renderPreviewDocument(substituteVariables(source, variables), tab === 0 ? effective : 'text'),
        [source, variables, effective, tab],
    );
    const isEmail = !channel || channel === 'EMAIL';

    return (
        <Card variant="outlined" sx={{ position: { lg: 'sticky' }, top: { lg: 72 } }}>
            <CardContent>
                <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap', mb: 1 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, flexGrow: 1 }}>
                        {translate('app.preview.title')}
                    </Typography>
                    <ToggleButtonGroup size="small" exclusive value={format} onChange={(_, value) => value && setFormat(value)}>
                        <ToggleButton value="auto">{translate('app.preview.auto', { format: detected.toUpperCase() })}</ToggleButton>
                        <ToggleButton value="html">HTML</ToggleButton>
                        <ToggleButton value="markdown">MD</ToggleButton>
                        <ToggleButton value="text">{translate('app.preview.text')}</ToggleButton>
                    </ToggleButtonGroup>
                    <ToggleButtonGroup size="small" exclusive value={width} onChange={(_, value) => value && setWidth(value)}>
                        <ToggleButton value="desktop">
                            <Tooltip title={translate('app.preview.desktop')}>
                                <DesktopIcon fontSize="small" />
                            </Tooltip>
                        </ToggleButton>
                        <ToggleButton value="mobile">
                            <Tooltip title={translate('app.preview.mobile')}>
                                <PhoneIcon fontSize="small" />
                            </Tooltip>
                        </ToggleButton>
                    </ToggleButtonGroup>
                </Stack>
                {effective === 'markdown' && (
                    <Alert severity="info" sx={{ mb: 1 }}>
                        {translate('app.preview.markdown_note')}
                    </Alert>
                )}
                {names.length > 0 && (
                    <Grid container spacing={1} sx={{ mb: 1 }}>
                        {names.map(name => (
                            <Grid key={name} size={{ xs: 6, md: 4 }}>
                                <TextField
                                    size="small"
                                    fullWidth
                                    label={name}
                                    placeholder={sample(name)}
                                    value={values[name] ?? ''}
                                    onChange={event => setValues({ ...values, [name]: event.target.value })}
                                />
                            </Grid>
                        ))}
                    </Grid>
                )}
                <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, overflow: 'hidden' }}>
                    <Box sx={{ px: 1.5, py: 1, bgcolor: 'action.hover', borderBottom: 1, borderColor: 'divider' }}>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            {translate('app.field.subject')}
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600, wordBreak: 'break-word' }}>
                            {deferred.subject ? substituteVariables(deferred.subject, variables) : '—'}
                        </Typography>
                    </Box>
                    {isEmail && (
                        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ minHeight: 36, '& .MuiTab-root': { minHeight: 36, py: 0 } }}>
                            <Tab label={translate('app.field.html_body')} />
                            <Tab label={translate('app.field.text_body')} disabled={!deferred.textBody} />
                        </Tabs>
                    )}
                    <Box sx={{ display: 'flex', justifyContent: 'center', bgcolor: width === 'mobile' ? 'action.hover' : 'transparent', p: width === 'mobile' ? 1 : 0 }}>
                        <Box
                            component="iframe"
                            title="template-live-preview"
                            sandbox=""
                            srcDoc={document}
                            sx={{ width: WIDTHS[width], height: 420, border: 0, bgcolor: '#fff', borderRadius: width === 'mobile' ? 2 : 0 }}
                        />
                    </Box>
                </Box>
                <Stack direction="row" spacing={0.5} useFlexGap sx={{ mt: 1, flexWrap: 'wrap', alignItems: 'center' }}>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                        {translate('app.preview.hint')}
                    </Typography>
                    {deferred.body.length > 0 && <Chip size="small" variant="outlined" label={`${deferred.body.length} chars`} />}
                </Stack>
            </CardContent>
        </Card>
    );
};
