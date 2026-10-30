import { useDataProvider } from 'react-admin';
import type { NotificationDataProvider } from './dataProvider';

/** Data provider with the custom (non-CRUD) calls of the notification service. */
export const useNotificationApi = () => useDataProvider<NotificationDataProvider>();
