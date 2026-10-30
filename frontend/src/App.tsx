import type { ReactNode } from 'react';
import { Admin, Authenticated, CustomRoutes, Resource, defaultDarkTheme, defaultLightTheme } from 'react-admin';
import { Route } from 'react-router-dom';
import { dataProvider } from './api/dataProvider';
import { authProvider } from './auth/authProvider';
import { LoginPage } from './auth/LoginPage';
import { i18nProvider } from './i18n';
import { Layout } from './layout/Layout';
import { config } from './config';
import { notifications, BatchSend } from './resources/notifications';
import { templates } from './resources/templates';
import { PreferencesPage } from './resources/preferences/PreferencesPage';
import { Dashboard } from './pages/Dashboard';
import { StatisticsPage } from './pages/StatisticsPage';
import { ChannelsPage } from './pages/ChannelsPage';
import { SystemPage } from './pages/SystemPage';

// Custom pages check the session like resource views do. Admin's requireAuth is not used:
// with a custom login page it re-renders the login page in a loop (react-admin 5.15).
const Private = ({ children }: { children: ReactNode }) => <Authenticated>{children}</Authenticated>;

export const App = () => (
    <Admin
        title={config.title}
        dataProvider={dataProvider}
        authProvider={authProvider}
        i18nProvider={i18nProvider}
        loginPage={LoginPage}
        layout={Layout}
        dashboard={Dashboard}
        theme={defaultLightTheme}
        darkTheme={defaultDarkTheme}
        disableTelemetry
    >
        <Resource name="notifications" {...notifications} options={{ label: 'app.menu.notifications' }} />
        <Resource name="templates" {...templates} options={{ label: 'app.menu.templates' }} />
        <Resource name="preferences" options={{ label: 'app.menu.preferences' }} />
        <CustomRoutes>
            <Route path="/notifications/batch" element={<Private><BatchSend /></Private>} />
            <Route path="/preferences" element={<Private><PreferencesPage /></Private>} />
            <Route path="/statistics" element={<Private><StatisticsPage /></Private>} />
            <Route path="/channels" element={<Private><ChannelsPage /></Private>} />
            <Route path="/system" element={<Private><SystemPage /></Private>} />
        </CustomRoutes>
    </Admin>
);
