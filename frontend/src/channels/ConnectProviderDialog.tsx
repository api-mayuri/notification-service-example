import { useMemo, useState } from 'react';
import { useTranslate } from 'react-admin';
import {
    Alert,
    Box,
    Button,
    Card,
    CardActionArea,
    CardContent,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    Grid,
    Link,
    Stack,
    Step,
    StepLabel,
    Stepper,
    Switch,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import { CodeBlock } from '../components/CodeBlock';
import { channelInfo, currentProviderId, envSnippet, javaSkeleton, providerEnv, providersFor, yamlSnippet } from './registry';
import type { ProviderDescriptor } from './types';

export const ProviderBadges = ({ provider, current }: { provider: ProviderDescriptor; current?: boolean }) => {
    const translate = useTranslate();
    return (
        <Stack sx={{ flexWrap: 'wrap' }} direction="row" spacing={0.5} useFlexGap>
            {current && <Chip size="small" color="primary" label={translate('app.provider.current')} />}
            <Chip
                size="small"
                variant="outlined"
                color={provider.kind === 'stub' ? 'warning' : 'success'}
                label={translate(`app.provider.kind.${provider.kind}`)}
            />
            <Chip
                size="small"
                variant="outlined"
                color={provider.availability === 'built-in' ? 'default' : 'secondary'}
                label={translate(`app.provider.availability.${provider.availability}`)}
            />
        </Stack>
    );
};

const ProviderChoice = ({ channel, selected, onSelect }: { channel: string; selected?: string; onSelect: (p: ProviderDescriptor) => void }) => {
    const translate = useTranslate();
    const current = currentProviderId(channel);
    return (
        <Grid container spacing={2}>
            {providersFor(channel).map(provider => (
                <Grid key={provider.id} size={{ xs: 12, md: 6 }}>
                    <Card variant="outlined" sx={{ borderColor: provider.id === selected ? 'primary.main' : undefined, height: '100%' }}>
                        <CardActionArea onClick={() => onSelect(provider)} sx={{ height: '100%' }}>
                            <CardContent>
                                <Typography sx={{ fontWeight: 600 }} variant="subtitle1">
                                    {provider.name}{' '}
                                    <Typography component="span" variant="caption" color="text.secondary">
                                        ({provider.id})
                                    </Typography>
                                </Typography>
                                <Box sx={{ my: 1 }}>
                                    <ProviderBadges provider={provider} current={provider.id === current} />
                                </Box>
                                <Typography variant="body2" color="text.secondary">
                                    {translate(provider.description)}
                                </Typography>
                            </CardContent>
                        </CardActionArea>
                    </Card>
                </Grid>
            ))}
        </Grid>
    );
};

const ProviderSettingsForm = ({
    provider,
    values,
    onChange,
}: {
    provider: ProviderDescriptor;
    values: Record<string, string>;
    onChange: (values: Record<string, string>) => void;
}) => {
    const translate = useTranslate();
    if (!provider.settings.length) {
        return <Alert severity="info">{translate('app.provider.no_settings')}</Alert>;
    }
    return (
        <Grid container spacing={2}>
            {provider.settings.map(setting => {
                const value = values[setting.env] ?? setting.defaultValue ?? '';
                const label = `${translate(setting.label, { _: setting.label })}${setting.required ? ' *' : ''}`;
                return (
                    <Grid key={setting.env} size={{ xs: 12, md: 6 }}>
                        {setting.type === 'boolean' ? (
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={value === 'true'}
                                        onChange={event => onChange({ ...values, [setting.env]: String(event.target.checked) })}
                                    />
                                }
                                label={`${label} (${setting.env})`}
                            />
                        ) : (
                            <TextField
                                fullWidth
                                size="small"
                                label={label}
                                type={setting.secret ? 'password' : setting.type === 'number' ? 'number' : 'text'}
                                placeholder={setting.placeholder}
                                value={value}
                                onChange={event => onChange({ ...values, [setting.env]: event.target.value })}
                                helperText={`${setting.env} → ${setting.property}${setting.help ? ` · ${translate(setting.help, { _: setting.help })}` : ''}`}
                            />
                        )}
                    </Grid>
                );
            })}
        </Grid>
    );
};

const Result = ({ provider, values }: { provider: ProviderDescriptor; values: Record<string, string> }) => {
    const translate = useTranslate();
    const [tab, setTab] = useState(0);
    const info = channelInfo(provider.channel);
    const planned = provider.availability === 'planned';
    const missing = provider.settings.filter(s => s.required && !(values[s.env] ?? s.defaultValue));
    return (
        <Box>
            {missing.length > 0 && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                    {translate('app.provider.missing_settings', { settings: missing.map(s => s.env).join(', ') })}
                </Alert>
            )}
            <Alert severity="info" sx={{ mb: 2 }}>
                {translate(planned ? 'app.provider.steps_planned' : 'app.provider.steps_built_in', {
                    env: providerEnv(provider.channel),
                    id: provider.id,
                    iface: info.providerInterface,
                    pkg: info.packageName,
                })}
            </Alert>
            <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 1 }}>
                <Tab label=".env" />
                <Tab label="application.yml" />
                {planned && <Tab label="Java" />}
            </Tabs>
            {tab === 0 && <CodeBlock code={envSnippet(provider, values)} />}
            {tab === 1 && <CodeBlock code={yamlSnippet(provider)} />}
            {tab === 2 && planned && <CodeBlock code={javaSkeleton(provider)} maxHeight={480} />}
            {provider.docsUrl && (
                <Typography sx={{ mt: 1 }} variant="body2">
                    <Link href={provider.docsUrl} target="_blank" rel="noreferrer">
                        {translate('app.provider.provider_docs')}
                    </Link>
                </Typography>
            )}
        </Box>
    );
};

/**
 * Wizard: choose a provider → fill settings → get .env / application.yml / Java skeleton.
 * Backend settings come only from the environment, so nothing is saved on the server.
 */
export const ConnectProviderDialog = ({ channel, open, onClose }: { channel: string; open: boolean; onClose: () => void }) => {
    const translate = useTranslate();
    const providers = useMemo(() => providersFor(channel), [channel]);
    const [step, setStep] = useState(0);
    const [provider, setProvider] = useState<ProviderDescriptor | undefined>(
        () => providers.find(p => p.availability === 'planned') ?? providers[0],
    );
    const [values, setValues] = useState<Record<string, string>>({});
    const steps = ['app.provider.step_choose', 'app.provider.step_settings', 'app.provider.step_apply'];
    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>{translate('app.provider.connect_title', { channel })}</DialogTitle>
            <DialogContent>
                <Alert severity="warning" sx={{ mb: 2 }}>
                    {translate('app.provider.env_only')}
                </Alert>
                <Stepper activeStep={step} sx={{ mb: 3 }}>
                    {steps.map(label => (
                        <Step key={label}>
                            <StepLabel>{translate(label)}</StepLabel>
                        </Step>
                    ))}
                </Stepper>
                {step === 0 && (
                    <ProviderChoice
                        channel={channel}
                        selected={provider?.id}
                        onSelect={selected => {
                            setProvider(selected);
                            setValues({});
                        }}
                    />
                )}
                {step === 1 && provider && <ProviderSettingsForm provider={provider} values={values} onChange={setValues} />}
                {step === 2 && provider && <Result provider={provider} values={values} />}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>{translate('ra.action.close')}</Button>
                <Button disabled={step === 0} onClick={() => setStep(step - 1)}>
                    {translate('ra.action.back')}
                </Button>
                {step < 2 && (
                    <Button variant="contained" disabled={!provider} onClick={() => setStep(step + 1)}>
                        {translate('app.action.next')}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
};
