import type { ReactNode } from 'react';
import {
    AppBar,
    Layout as RaLayout,
    LocalesMenuButton,
    Menu,
    RefreshIconButton,
    TitlePortal,
    ToggleThemeButton,
    usePermissions,
    useTranslate,
} from 'react-admin';
import { Box, Chip } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import NotificationsIcon from '@mui/icons-material/Notifications';
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd';
import DescriptionIcon from '@mui/icons-material/Description';
import TuneIcon from '@mui/icons-material/Tune';
import BarChartIcon from '@mui/icons-material/BarChart';
import HubIcon from '@mui/icons-material/Hub';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import { SCOPES } from '../api/constants';
import { hasScope } from '../auth/authProvider';
import { tokenStorage } from '../auth/tokenStorage';

/** Menu items are shown only for the scopes of the current token. */
const AppMenu = () => {
    const { permissions } = usePermissions();
    const can = (scope: string) => hasScope(permissions, scope);
    return (
        <Menu>
            <Menu.Item to="/" primaryText="app.menu.dashboard" leftIcon={<DashboardIcon />} />
            {can(SCOPES.read) && <Menu.Item to="/notifications" primaryText="app.menu.notifications" leftIcon={<NotificationsIcon />} />}
            {can(SCOPES.send) && <Menu.Item to="/notifications/batch" primaryText="app.menu.batch" leftIcon={<PlaylistAddIcon />} />}
            {can(SCOPES.templates) && <Menu.Item to="/templates" primaryText="app.menu.templates" leftIcon={<DescriptionIcon />} />}
            {can(SCOPES.preferences) && <Menu.Item to="/preferences" primaryText="app.menu.preferences" leftIcon={<TuneIcon />} />}
            {can(SCOPES.read) && <Menu.Item to="/statistics" primaryText="app.menu.statistics" leftIcon={<BarChartIcon />} />}
            <Menu.Item to="/channels" primaryText="app.menu.channels" leftIcon={<HubIcon />} />
            <Menu.Item to="/system" primaryText="app.menu.system" leftIcon={<MonitorHeartIcon />} />
        </Menu>
    );
};

const ClientBadge = () => {
    const translate = useTranslate();
    const auth = tokenStorage.get();
    if (!auth) return null;
    return (
        <Chip
            size="small"
            variant="outlined"
            label={`${translate('app.field.clientId')}: ${auth.clientId}`}
            sx={{ color: 'inherit', borderColor: 'currentColor', mr: 1, display: { xs: 'none', md: 'flex' } }}
        />
    );
};

const AppAppBar = () => (
    <AppBar
        toolbar={
            <>
                <ClientBadge />
                <LocalesMenuButton />
                <ToggleThemeButton />
                <RefreshIconButton />
            </>
        }
    >
        <TitlePortal />
        <Box sx={{ flex: 1 }} />
    </AppBar>
);

export const Layout = ({ children }: { children: ReactNode }) => (
    <RaLayout menu={AppMenu} appBar={AppAppBar}>
        {children}
    </RaLayout>
);
