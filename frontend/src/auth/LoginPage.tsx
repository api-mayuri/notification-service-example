import { useState } from 'react';
import { useLogin, useNotify, useTranslate } from 'react-admin';
import {
    Avatar,
    Box,
    Button,
    Card,
    CardContent,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { config } from '../config';
import { ALL_SCOPES } from '../api/constants';
import type { LoginParams } from './authProvider';

export const LoginPage = () => {
    const translate = useTranslate();
    const login = useLogin();
    const notify = useNotify();
    const devAllowed = config.authMode === 'dev-token';
    const [tab, setTab] = useState(devAllowed ? 0 : 1);
    const [clientId, setClientId] = useState(config.devTokenClientId);
    const [scopes, setScopes] = useState<string[]>(ALL_SCOPES);
    const [token, setToken] = useState('');
    const [loading, setLoading] = useState(false);

    const submit = async (params: LoginParams) => {
        setLoading(true);
        try {
            await login(params);
        } catch (error) {
            notify((error as Error).message || 'ra.auth.sign_in_error', { type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: theme => `linear-gradient(135deg, ${theme.palette.primary.dark}, ${theme.palette.secondary.main})`,
                p: 2,
            }}
        >
            <Card sx={{ width: 440, maxWidth: '100%' }}>
                <CardContent>
                    <Box display="flex" flexDirection="column" alignItems="center" mb={2}>
                        <Avatar sx={{ bgcolor: 'primary.main', mb: 1 }}>
                            <NotificationsIcon />
                        </Avatar>
                        <Typography variant="h5">{config.title}</Typography>
                    </Box>
                    <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="fullWidth" sx={{ mb: 2 }}>
                        {devAllowed && <Tab value={0} label={translate('app.auth.dev_token')} />}
                        <Tab value={1} label={translate('app.auth.bearer')} />
                    </Tabs>
                    {tab === 0 && devAllowed ? (
                        <Box
                            component="form"
                            onSubmit={event => {
                                event.preventDefault();
                                submit({ mode: 'dev-token', clientId, scopes });
                            }}
                        >
                            <Typography variant="body2" color="text.secondary" mb={2}>
                                {translate('app.auth.dev_token_help')}
                            </Typography>
                            <TextField
                                label="clientId"
                                value={clientId}
                                onChange={event => setClientId(event.target.value)}
                                fullWidth
                                required
                                size="small"
                                sx={{ mb: 1 }}
                            />
                            <FormGroup>
                                {ALL_SCOPES.map(scope => (
                                    <FormControlLabel
                                        key={scope}
                                        control={
                                            <Checkbox
                                                size="small"
                                                checked={scopes.includes(scope)}
                                                onChange={event =>
                                                    setScopes(event.target.checked ? [...scopes, scope] : scopes.filter(s => s !== scope))
                                                }
                                            />
                                        }
                                        label={<Typography variant="body2">{scope}</Typography>}
                                    />
                                ))}
                            </FormGroup>
                            <Button type="submit" variant="contained" fullWidth disabled={loading || !clientId.trim()} sx={{ mt: 2 }}>
                                {translate('ra.auth.sign_in')}
                            </Button>
                        </Box>
                    ) : (
                        <Box
                            component="form"
                            onSubmit={event => {
                                event.preventDefault();
                                submit({ mode: 'bearer', token });
                            }}
                        >
                            <Typography variant="body2" color="text.secondary" mb={2}>
                                {translate('app.auth.bearer_help')}
                            </Typography>
                            <TextField
                                label="Access token (JWT)"
                                value={token}
                                onChange={event => setToken(event.target.value)}
                                fullWidth
                                required
                                multiline
                                minRows={4}
                                slotProps={{ input: { sx: { fontFamily: 'monospace', fontSize: 12 } } }}
                            />
                            <Button type="submit" variant="contained" fullWidth disabled={loading || !token.trim()} sx={{ mt: 2 }}>
                                {translate('ra.auth.sign_in')}
                            </Button>
                        </Box>
                    )}
                </CardContent>
            </Card>
        </Box>
    );
};
