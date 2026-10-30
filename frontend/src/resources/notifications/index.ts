import NotificationsIcon from '@mui/icons-material/Notifications';
import { NotificationList } from './NotificationList';
import { NotificationShow } from './NotificationShow';
import { NotificationCreate } from './NotificationCreate';

export { BatchSend } from './BatchSend';

export const notifications = {
    list: NotificationList,
    show: NotificationShow,
    create: NotificationCreate,
    icon: NotificationsIcon,
    recordRepresentation: 'id',
};
