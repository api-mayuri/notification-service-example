// Mirrors backend DTOs (application/port/in/*View, infrastructure/in/rest/dto/*).

export type NotificationStatus = 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PARTIALLY_FAILED' | 'FAILED' | 'CANCELLED';
export type DeliveryStatus =
    | 'PENDING'
    | 'SENDING'
    | 'RETRY_SCHEDULED'
    | 'SENT'
    | 'DELIVERED'
    | 'FAILED'
    | 'SKIPPED'
    | 'CANCELLED';
export type Category = 'TRANSACTIONAL' | 'MARKETING' | 'SYSTEM';
export type Priority = 'LOW' | 'NORMAL' | 'HIGH';
export type TemplateStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type Granularity = 'HOUR' | 'DAY';
export type SendOutcome = 'CREATED' | 'REPLAYED' | 'DEDUPLICATED';

export interface DeliveryView {
    id: string;
    channel: string;
    status: DeliveryStatus;
    maskedAddress: string | null;
    attempts: number;
    nextAttemptAt: string | null;
    lastError: string | null;
    providerMessageId: string | null;
    simulated: boolean;
    skipReason: string | null;
    templateVersion: number | null;
    templateLocale: string | null;
    sentAt: string | null;
    updatedAt: string;
}

export interface NotificationView {
    id: string;
    clientId: string;
    recipientId: string;
    status: NotificationStatus;
    category: Category;
    priority: Priority;
    channels: string[];
    templateKey: string | null;
    sendAt: string | null;
    metadata: Record<string, string> | null;
    createdAt: string;
    updatedAt: string;
    deliveries: DeliveryView[];
}

export interface NotificationSummaryView {
    id: string;
    recipientId: string;
    status: NotificationStatus;
    category: Category;
    priority: Priority;
    channels: string[];
    templateKey: string | null;
    createdAt: string;
}

export interface PageResponse<T> {
    items: T[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
}

export interface NotificationRequest {
    recipient: { id: string; contacts?: Record<string, string> };
    template?: { key: string; locale?: string; variables?: Record<string, unknown> };
    content?: { subject?: string; text?: string; html?: string };
    channels: string[];
    category: Category;
    priority?: Priority;
    sendAt?: string;
    metadata?: Record<string, string>;
    idempotencyKey?: string;
}

export interface NotificationAccepted {
    id: string;
    status: NotificationStatus;
    outcome: SendOutcome;
}

export interface BatchItemResult {
    index: number;
    status: number;
    notificationId: string | null;
    notificationStatus: string | null;
    outcome: SendOutcome | null;
    errorCode: string | null;
    detail: string | null;
}

export interface BatchResponse {
    accepted: number;
    rejected: number;
    items: BatchItemResult[];
}

export interface TemplateVersion {
    version: number;
    status: TemplateStatus;
    subject: string | null;
    body: string;
    textBody: string | null;
    requiredVariables: string[];
    createdAt: string;
}

export interface TemplateView {
    id: string;
    key: string;
    channel: string;
    locale: string;
    activeVersion: number | null;
    /** ISO 8601 duration (PT10M); null — deduplication off. */
    deduplicationWindow: string | number | null;
    versions: TemplateVersion[];
    createdAt: string;
    updatedAt: string;
}

export interface TemplateContentRequest {
    subject?: string | null;
    body: string;
    textBody?: string | null;
    requiredVariables?: string[];
}

export interface CreateTemplateRequest extends TemplateContentRequest {
    key: string;
    channel: string;
    locale: string;
    deduplicationWindow?: string | null;
    activate: boolean;
}

export interface AddVersionRequest extends TemplateContentRequest {
    channel: string;
    locale: string;
    activate: boolean;
}

export interface TemplateVariant {
    key: string;
    channel: string;
    locale: string;
}

export interface TemplatePreview {
    subject: string | null;
    textBody: string | null;
    htmlBody: string | null;
    version: number;
    locale: string;
}

export interface PreferencesView {
    recipientId: string;
    unsubscribedChannels: string[];
    unsubscribedCategories: string[];
    quietHoursStart: string | null;
    quietHoursEnd: string | null;
    timeZone: string | null;
    locale: string | null;
    updatedAt: string | null;
}

export interface PreferencesRequest {
    unsubscribedChannels?: string[] | null;
    unsubscribedCategories?: string[] | null;
    quietHoursStart?: string | null;
    quietHoursEnd?: string | null;
    timeZone?: string | null;
    locale?: string | null;
    clearQuietHours?: boolean;
    clearLocale?: boolean;
}

export interface StatisticsRow {
    periodStart: string;
    channel: string;
    status: string;
    count: number;
}

export interface StatisticsView {
    from: string;
    to: string;
    granularity: Granularity;
    totals: Record<string, Record<string, number>>;
    rows: StatisticsRow[];
}

export interface StatisticsQuery {
    from?: string;
    to?: string;
    channel?: string;
    granularity?: Granularity;
}

/** RFC 9457 Problem Details with backend extensions. */
export interface ProblemDetail {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    errorCode?: string;
    missingVariables?: string[];
    errors?: { field: string; message: string }[];
}

export interface HealthComponent {
    status: string;
    details?: Record<string, unknown>;
    components?: Record<string, HealthComponent>;
}

export interface MetricResponse {
    name: string;
    description?: string;
    baseUnit?: string;
    measurements: { statistic: string; value: number }[];
    availableTags: { tag: string; values: string[] }[];
}

export interface DevTokenResponse {
    access_token: string;
    token_type: string;
    expires_in: number;
    scope: string;
}
