import { Admin, CustomRoutes, Resource, defaultDarkTheme, defaultLightTheme } from 'react-admin';
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
        requireAuth
        disableTelemetry
    >
        <Resource name="notifications" {...notifications} options={{ label: 'app.menu.notifications' }} />
        <Resource name="templates" {...templates} options={{ label: 'app.menu.templates' }} />
        <Resource name="preferences" options={{ label: 'app.menu.preferences' }} />
        <CustomRoutes>
            <Route path="/notifications/batch" element={<BatchSend />} />
            <Route path="/preferences" element={<PreferencesPage />} />
            <Route path="/statistics" element={<StatisticsPage />} />
            <Route path="/channels" element={<ChannelsPage />} />
            <Route path="/system" element={<SystemPage />} />
        </CustomRoutes>
    </Admin>
);
