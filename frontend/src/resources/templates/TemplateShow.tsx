import { useEffect, useState } from 'react';
import {
    DateField,
    Labeled,
    ListButton,
    RefreshButton,
    Show,
    TextField,
    TopToolbar,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Grid,
    Stack,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Tabs,
    Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VisibilityIcon from '@mui/icons-material/Visibility';
import TimerIcon from '@mui/icons-material/Timer';
import { Link } from 'react-router-dom';
import { useNotificationApi } from '../../api/useNotificationApi';
import type { TemplateVersion, TemplateView } from '../../api/types';
import { ChannelChip } from '../../components/ChannelChip';
import { CodeBlock } from '../../components/CodeBlock';
import { HtmlPreview } from '../../components/HtmlPreview';
import { StatusChip } from '../../components/StatusChip';
import { humanizeDuration } from '../../utils/duration';
import { AddVersionDialog, SettingsDialog, useDialog } from './TemplateDialogs';
import { TemplatePreviewPanel } from './TemplatePreviewPanel';

const Actions = ({ onAddVersion, onSettings }: { onAddVersion: () => void; onSettings: () => void }) => {
    const translate = useTranslate();
    return (
        <TopToolbar>
            <ListButton />
            <RefreshButton />
            <Button size="small" startIcon={<TimerIcon />} onClick={onSettings}>
                {translate('app.action.template_settings')}
            </Button>
            <Button size="small" startIcon={<AddIcon />} onClick={onAddVersion}>
                {translate('app.action.add_version')}
            </Button>
        </TopToolbar>
    );
};

const Title = () => {
    const record = useRecordContext<TemplateView>();
    const translate = useTranslate();
    return <span>{record ? translate('app.page.template', { key: record.key, channel: record.channel, locale: record.locale }) : ''}</span>;
};

const ActivateButton = ({ version }: { version: TemplateVersion }) => {
    const record = useRecordContext<TemplateView>();
    const api = useNotificationApi();
    const notify = useNotify();
    const refresh = useRefresh();
    const translate = useTranslate();
    const [loading, setLoading] = useState(false);
    if (!record || version.status === 'ACTIVE') return null;
    return (
        <Button
            size="small"
            color="success"
            startIcon={<CheckCircleIcon />}
            disabled={loading}
            onClick={async event => {
                event.stopPropagation();
                setLoading(true);
                try {
                    await api.activateTemplateVersion(record.key, { channel: record.channel, locale: record.locale }, version.version);
                    notify('app.message.version_activated', { type: 'success', messageArgs: { version: version.version } });
                    refresh();
                } catch (error) {
                    notify((error as Error).message, { type: 'error' });
                } finally {
                    setLoading(false);
                }
            }}
        >
            {translate('app.action.activate')}
        </Button>
    );
};

const Versions = ({ onPreview, onCopy }: { onPreview: (v: number) => void; onCopy: (v: TemplateVersion) => void }) => {
    const record = useRecordContext<TemplateView>();
    const translate = useTranslate();
    if (!record) return null;
    const versions = [...record.versions].sort((a, b) => b.version - a.version);
    return (
        <Box>
            {versions.map(version => (
                // status in the key: remount (and re-apply defaultExpanded) when a version gets activated
                <Accordion key={`${version.version}-${version.status}`} variant="outlined" defaultExpanded={version.status === 'ACTIVE'} disableGutters>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Stack direction="row" spacing={1} alignItems="center" width="100%" flexWrap="wrap" useFlexGap>
                            <Typography fontWeight={600}>v{version.version}</Typography>
                            <StatusChip status={version.status} />
                            <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }} noWrap>
                                {version.subject ?? ''}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {new Date(version.createdAt).toLocaleString()}
                            </Typography>
                        </Stack>
                    </AccordionSummary>
                    <AccordionDetails>
                        <Stack direction="row" spacing={1} mb={2} useFlexGap flexWrap="wrap">
                            <ActivateButton version={version} />
                            <Button size="small" startIcon={<VisibilityIcon />} onClick={() => onPreview(version.version)}>
                                {translate('app.action.preview')}
                            </Button>
                            <Button size="small" startIcon={<ContentCopyIcon />} onClick={() => onCopy(version)}>
                                {translate('app.action.new_version_from')}
                            </Button>
                        </Stack>
                        <Typography variant="subtitle2">{translate('app.field.requiredVariables')}</Typography>
                        <Stack direction="row" spacing={0.5} mb={2} useFlexGap flexWrap="wrap">
                            {version.requiredVariables.length ? (
                                version.requiredVariables.map(name => <Chip key={name} size="small" label={name} />)
                            ) : (
                                <Typography variant="body2">—</Typography>
                            )}
                        </Stack>
                        <Grid container spacing={2}>
                            <Grid size={{ xs: 12, md: record.channel === 'EMAIL' ? 6 : 12 }}>
                                <Typography variant="subtitle2">{translate(record.channel === 'EMAIL' ? 'app.field.html_body' : 'app.field.body')}</Typography>
                                <CodeBlock code={version.body} maxHeight={320} />
                                {version.textBody && (
                                    <Box mt={2}>
                                        <Typography variant="subtitle2">{translate('app.field.text_body')}</Typography>
                                        <CodeBlock code={version.textBody} maxHeight={200} />
                                    </Box>
                                )}
                            </Grid>
                            {record.channel === 'EMAIL' && (
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <Typography variant="subtitle2">{translate('app.section.raw_html')}</Typography>
                                    <HtmlPreview html={version.body} />
                                </Grid>
                            )}
                        </Grid>
                    </AccordionDetails>
                </Accordion>
            ))}
        </Box>
    );
};

/** GET /templates/{key}: all channel/locale variants of the key. */
const Variants = () => {
    const record = useRecordContext<TemplateView>();
    const api = useNotificationApi();
    const translate = useTranslate();
    const [variants, setVariants] = useState<TemplateView[] | null>(null);
    const key = record?.key;
    useEffect(() => {
        if (!key) return;
        api.getTemplatesByKey(key).then(setVariants, () => setVariants([]));
        // api is a new proxy on every render; refetch only when the key changes
    }, [key]);
    if (!record || !variants) return null;
    return (
        <Table size="small">
            <TableHead>
                <TableRow>
                    <TableCell>{translate('app.field.channel')}</TableCell>
                    <TableCell>{translate('app.field.locale')}</TableCell>
                    <TableCell>{translate('app.field.activeVersion')}</TableCell>
                    <TableCell>{translate('app.field.versions')}</TableCell>
                    <TableCell>{translate('app.field.deduplicationWindow')}</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {variants.map(variant => (
                    <TableRow key={variant.id} selected={variant.id === record.id} hover>
                        <TableCell>
                            <Link to={`/templates/${variant.id}/show`}>
                                <ChannelChip channel={variant.channel} clickable />
                            </Link>
                        </TableCell>
                        <TableCell>{variant.locale}</TableCell>
                        <TableCell>{variant.activeVersion ? `v${variant.activeVersion}` : '—'}</TableCell>
                        <TableCell>{variant.versions.length}</TableCell>
                        <TableCell>{humanizeDuration(variant.deduplicationWindow)}</TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
};

const Header = () => {
    const record = useRecordContext<TemplateView>();
    if (!record) return null;
    return (
        <Grid container spacing={2}>
            <Grid size={{ xs: 6, md: 3 }}>
                <Labeled label="app.field.key">
                    <TextField source="key" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
                <Labeled label="app.field.channel">
                    <ChannelChip channel={record.channel} />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
                <Labeled label="app.field.locale">
                    <TextField source="locale" />
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 1 }}>
                <Labeled label="app.field.activeVersion">
                    <span>{record.activeVersion ? `v${record.activeVersion}` : '—'}</span>
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
                <Labeled label="app.field.deduplicationWindow">
                    <span>{humanizeDuration(record.deduplicationWindow)}</span>
                </Labeled>
            </Grid>
            <Grid size={{ xs: 6, md: 2 }}>
                <Labeled label="app.field.updatedAt">
                    <DateField source="updatedAt" showTime />
                </Labeled>
            </Grid>
        </Grid>
    );
};

const Body = () => {
    const translate = useTranslate();
    const [tab, setTab] = useState(0);
    const [previewVersion, setPreviewVersion] = useState<number | null>(null);
    const [base, setBase] = useState<TemplateVersion | null>(null);
    const addVersion = useDialog();
    const settings = useDialog();
    return (
        <>
            <Actions
                onAddVersion={() => {
                    setBase(null);
                    addVersion.show();
                }}
                onSettings={settings.show}
            />
            <Card variant="outlined" sx={{ m: 2 }}>
                <CardContent>
                    <Header />
                </CardContent>
            </Card>
            <Box px={2} pb={2}>
                <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
                    <Tab label={translate('app.section.versions')} />
                    <Tab label={translate('app.section.preview')} />
                    <Tab label={translate('app.section.variants')} />
                </Tabs>
                {tab === 0 && (
                    <Versions
                        onPreview={version => {
                            setPreviewVersion(version);
                            setTab(1);
                        }}
                        onCopy={version => {
                            setBase(version);
                            addVersion.show();
                        }}
                    />
                )}
                {tab === 1 && <TemplatePreviewPanel initialVersion={previewVersion} />}
                {tab === 2 && <Variants />}
            </Box>
            {addVersion.open && <AddVersionDialog open onClose={addVersion.close} base={base} />}
            {settings.open && <SettingsDialog open onClose={settings.close} />}
        </>
    );
};

export const TemplateShow = () => (
    <Show title={<Title />} actions={false}>
        <Body />
    </Show>
);
