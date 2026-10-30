import { config } from '../config';
import type { Category, DeliveryStatus, NotificationStatus, Priority, TemplateStatus } from './types';

export const BUILT_IN_CHANNELS = ['EMAIL', 'SMS', 'TELEGRAM', 'WHATSAPP', 'PUSH', 'WEBHOOK'] as const;

/** Built-in channels plus EXTRA_CHANNELS from the config (ChannelType is open on the backend). */
export const CHANNELS: string[] = [...BUILT_IN_CHANNELS, ...config.extraChannels.filter(c => !BUILT_IN_CHANNELS.includes(c as never))];

/** Contact type the channel needs (ChannelSender.contactType on the backend). */
export const CHANNEL_CONTACT: Record<string, string> = {
    EMAIL: 'email',
    SMS: 'phone',
    WHATSAPP: 'phone',
    TELEGRAM: 'telegramChatId',
    PUSH: 'deviceToken',
    WEBHOOK: 'webhookUrl',
};

export const CONTACT_TYPES = ['email', 'phone', 'telegramChatId', 'deviceToken', 'webhookUrl'] as const;

export const NOTIFICATION_STATUSES: NotificationStatus[] = [
    'ACCEPTED',
    'IN_PROGRESS',
    'COMPLETED',
    'PARTIALLY_FAILED',
    'FAILED',
    'CANCELLED',
];
export const TERMINAL_NOTIFICATION_STATUSES: NotificationStatus[] = ['COMPLETED', 'PARTIALLY_FAILED', 'FAILED', 'CANCELLED'];
export const DELIVERY_STATUSES: DeliveryStatus[] = [
    'PENDING',
    'SENDING',
    'RETRY_SCHEDULED',
    'SENT',
    'DELIVERED',
    'FAILED',
    'SKIPPED',
    'CANCELLED',
];
export const CATEGORIES: Category[] = ['TRANSACTIONAL', 'MARKETING', 'SYSTEM'];
/** Only optional categories can be unsubscribed from. */
export const OPTIONAL_CATEGORIES: Category[] = ['MARKETING'];
export const PRIORITIES: Priority[] = ['LOW', 'NORMAL', 'HIGH'];
export const TEMPLATE_STATUSES: TemplateStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
export const SKIP_REASONS = ['UNSUBSCRIBED_CHANNEL', 'UNSUBSCRIBED_CATEGORY', 'NO_CONTACT'];

export const SCOPES = {
    send: 'notifications:send',
    sendRaw: 'notifications:send-raw',
    read: 'notifications:read',
    templates: 'templates:manage',
    preferences: 'preferences:manage',
} as const;
export const ALL_SCOPES: string[] = Object.values(SCOPES);

/** Limits enforced by the backend. */
export const LIMITS = {
    pageSize: 200,
    batchSize: 500,
    statisticsDays: 92,
    metadataEntries: 20,
    channelsPerRequest: 16,
} as const;

type Color = 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';

export const STATUS_COLORS: Record<string, Color> = {
    ACCEPTED: 'info',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    PARTIALLY_FAILED: 'warning',
    FAILED: 'error',
    CANCELLED: 'default',
    PENDING: 'info',
    SENDING: 'primary',
    RETRY_SCHEDULED: 'warning',
    SENT: 'success',
    DELIVERED: 'success',
    SKIPPED: 'default',
    DRAFT: 'default',
    ACTIVE: 'success',
    ARCHIVED: 'secondary',
    UP: 'success',
    DOWN: 'error',
    OUT_OF_SERVICE: 'error',
    UNKNOWN: 'default',
    CLOSED: 'success',
    OPEN: 'error',
    HALF_OPEN: 'warning',
    CIRCUIT_OPEN: 'error',
    CIRCUIT_HALF_OPEN: 'warning',
};
