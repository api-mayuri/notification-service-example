import { Chip, Stack, type ChipProps } from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import SmsIcon from '@mui/icons-material/Sms';
import TelegramIcon from '@mui/icons-material/Telegram';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import PushIcon from '@mui/icons-material/NotificationsActive';
import WebhookIcon from '@mui/icons-material/Webhook';
import HubIcon from '@mui/icons-material/Hub';
import type { ReactElement } from 'react';
import { useRecordContext } from 'react-admin';

const ICONS: Record<string, ReactElement> = {
    EMAIL: <EmailIcon fontSize="small" />,
    SMS: <SmsIcon fontSize="small" />,
    TELEGRAM: <TelegramIcon fontSize="small" />,
    WHATSAPP: <WhatsAppIcon fontSize="small" />,
    PUSH: <PushIcon fontSize="small" />,
    WEBHOOK: <WebhookIcon fontSize="small" />,
};

export const channelIcon = (channel: string): ReactElement => ICONS[channel] ?? <HubIcon fontSize="small" />;

export const ChannelChip = ({ channel, ...rest }: { channel: string } & ChipProps) => (
    <Chip size="small" variant="outlined" icon={channelIcon(channel)} label={channel} {...rest} />
);

export const ChannelsField = ({ source }: { source: string; label?: string; sortable?: boolean }) => {
    const record = useRecordContext();
    const value = record?.[source];
    const channels: string[] = Array.isArray(value) ? value : value ? [value] : [];
    return (
        <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">
            {channels.map(channel => (
                <ChannelChip key={channel} channel={channel} />
            ))}
        </Stack>
    );
};
