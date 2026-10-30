import type {
    CreateParams,
    DataProvider,
    GetListParams,
    GetListResult,
    GetManyParams,
    GetOneParams,
    UpdateParams,
} from 'react-admin';
import { api, apiRequest } from './http';
import { LIMITS } from './constants';
import type {
    AddVersionRequest,
    BatchResponse,
    CreateTemplateRequest,
    HealthComponent,
    MetricResponse,
    NotificationAccepted,
    NotificationRequest,
    NotificationSummaryView,
    NotificationView,
    PageResponse,
    PreferencesRequest,
    PreferencesView,
    StatisticsQuery,
    StatisticsView,
    TemplatePreview,
    TemplateVariant,
    TemplateView,
} from './types';

const NOTIFICATIONS = '/api/v1/notifications';
const TEMPLATES = '/api/v1/templates';

export const newIdempotencyKey = (): string =>
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `ui-${Date.now()}-${Math.random().toString(16).slice(2)}`;

/** Date input value (Date, "2026-10-30T10:00", ISO) → ISO instant the backend accepts. */
export function toInstant(value: unknown): string | undefined {
    if (value === undefined || value === null || value === '') return undefined;
    const date = value instanceof Date ? value : new Date(String(value));
    return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

export interface AcceptedResult extends NotificationAccepted {
    replayed: boolean;
}

/** Custom calls beyond CRUD; available via useNotificationApi(). */
export interface NotificationApi {
    sendNotification(request: NotificationRequest, idempotencyKey: string): Promise<AcceptedResult>;
    sendBatch(items: NotificationRequest[], idempotencyKey: string): Promise<BatchResponse>;
    cancelNotification(id: string): Promise<NotificationView>;
    getTemplatesByKey(key: string): Promise<TemplateView[]>;
    addTemplateVersion(key: string, request: AddVersionRequest): Promise<TemplateView>;
    activateTemplateVersion(key: string, variant: Omit<TemplateVariant, 'key'>, version: number): Promise<TemplateView>;
    previewTemplate(
        key: string,
        request: { channel: string; locale: string; version?: number | null; variables: Record<string, unknown> },
    ): Promise<TemplatePreview>;
    patchPreferences(recipientId: string, request: PreferencesRequest): Promise<PreferencesView>;
    getStatistics(query: StatisticsQuery): Promise<StatisticsView>;
    getHealth(): Promise<HealthComponent>;
    getInfo(): Promise<Record<string, unknown>>;
    getMetric(name: string, tags?: Record<string, string>): Promise<MetricResponse | null>;
}

export type NotificationDataProvider = DataProvider & NotificationApi;

function unsupported(resource: string, method: string): never {
    throw new Error(`app.error.unsupported: ${resource}.${method}`);
}

function compare(a: unknown, b: unknown): number {
    if (a === b) return 0;
    if (a === null || a === undefined) return 1;
    if (b === null || b === undefined) return -1;
    return String(a).localeCompare(String(b), undefined, { numeric: true });
}

/** Templates API has no paging or filters: list everything once and filter in the browser. */
export function filterTemplates(templates: TemplateView[], params: GetListParams): GetListResult<TemplateView> {
    const filter = params.filter ?? {};
    const query = String(filter.q ?? '').trim().toLowerCase();
    let rows = templates.filter(
        template =>
            (!query || template.key.toLowerCase().includes(query)) &&
            (!filter.key || template.key === filter.key) &&
            (!filter.channel || template.channel === filter.channel) &&
            (!filter.locale || template.locale.toLowerCase() === String(filter.locale).toLowerCase()) &&
            (filter.hasActive === undefined || (template.activeVersion !== null) === filter.hasActive),
    );
    const { field, order } = params.sort ?? { field: 'key', order: 'ASC' };
    rows = [...rows].sort((a, b) => {
        const result = compare((a as never)[field], (b as never)[field]);
        return order === 'DESC' ? -result : result;
    });
    const { page, perPage } = params.pagination ?? { page: 1, perPage: rows.length || 1 };
    return { data: rows.slice((page - 1) * perPage, page * perPage), total: rows.length };
}

async function listTemplates(): Promise<TemplateView[]> {
    return api.get<TemplateView[]>(TEMPLATES);
}

const enc = encodeURIComponent;

export const dataProvider: NotificationDataProvider = {
    async getList(resource: string, params: GetListParams): Promise<GetListResult<any>> {
        if (resource === 'notifications') {
            const filter = params.filter ?? {};
            const { page, perPage } = params.pagination ?? { page: 1, perPage: 20 };
            // Backend: zero-based page, size 1..200, newest first (no custom sort).
            const size = Math.min(Math.max(perPage, 1), LIMITS.pageSize);
            const response = await api.get<PageResponse<NotificationSummaryView>>(NOTIFICATIONS, {
                recipientId: filter.recipientId,
                status: filter.status,
                channel: filter.channel,
                from: toInstant(filter.from),
                to: toInstant(filter.to),
                page: page - 1,
                size,
            });
            return {
                data: response.items,
                total: response.totalItems,
            };
        }
        if (resource === 'templates') {
            return filterTemplates(await listTemplates(), params);
        }
        return unsupported(resource, 'getList');
    },

    async getOne(resource: string, params: GetOneParams): Promise<any> {
        if (resource === 'notifications') {
            return { data: await api.get<NotificationView>(`${NOTIFICATIONS}/${enc(String(params.id))}`) };
        }
        if (resource === 'templates') {
            const template = (await listTemplates()).find(item => item.id === params.id);
            if (!template) {
                throw new Error('ra.notification.item_doesnt_exist');
            }
            return { data: template };
        }
        if (resource === 'preferences') {
            const view = await api.get<PreferencesView>(`/api/v1/recipients/${enc(String(params.id))}/preferences`);
            return { data: { ...view, id: view.recipientId } };
        }
        return unsupported(resource, 'getOne');
    },

    async getMany(resource: string, params: GetManyParams): Promise<any> {
        if (resource === 'templates') {
            const templates = await listTemplates();
            return { data: templates.filter(template => params.ids.includes(template.id)) };
        }
        const results = await Promise.all(params.ids.map(id => dataProvider.getOne(resource, { id })));
        return { data: results.map(result => result.data) };
    },

    async getManyReference(resource: string): Promise<any> {
        return unsupported(resource, 'getManyReference');
    },

    async create(resource: string, params: CreateParams): Promise<any> {
        if (resource === 'notifications') {
            const { idempotencyKey, ...request } = params.data as NotificationRequest & { idempotencyKey?: string };
            const key = (params.meta?.idempotencyKey as string | undefined) ?? idempotencyKey ?? newIdempotencyKey();
            const accepted = await dataProvider.sendNotification(request, key);
            // react-admin caches the create result as the record: store the full view, not the 202 body.
            const view = await api.get<NotificationView>(`${NOTIFICATIONS}/${enc(accepted.id)}`);
            return { data: { ...view, outcome: accepted.outcome, replayed: accepted.replayed } };
        }
        if (resource === 'templates') {
            const response = await api.post<TemplateView>(TEMPLATES, params.data as CreateTemplateRequest);
            return { data: response.body };
        }
        return unsupported(resource, 'create');
    },

    async update(resource: string, params: UpdateParams): Promise<any> {
        if (resource === 'templates') {
            const data = params.data as Partial<TemplateView> & { deduplicationWindow?: string | null };
            const previous = params.previousData as TemplateView;
            const view = await api.put<TemplateView>(`${TEMPLATES}/${enc(previous.key)}`, {
                channel: previous.channel,
                locale: previous.locale,
                deduplicationWindow: data.deduplicationWindow || null,
            });
            return { data: view };
        }
        if (resource === 'preferences') {
            const data = params.data as PreferencesRequest;
            const body: PreferencesRequest = {
                unsubscribedChannels: data.unsubscribedChannels ?? [],
                unsubscribedCategories: data.unsubscribedCategories ?? [],
                quietHoursStart: data.quietHoursStart || null,
                quietHoursEnd: data.quietHoursEnd || null,
                timeZone: data.timeZone || null,
                locale: data.locale || null,
            };
            const view = await api.put<PreferencesView>(`/api/v1/recipients/${enc(String(params.id))}/preferences`, body);
            return { data: { ...view, id: view.recipientId } };
        }
        return unsupported(resource, 'update');
    },

    async updateMany(resource: string): Promise<any> {
        return unsupported(resource, 'updateMany');
    },

    async delete(resource: string): Promise<any> {
        return unsupported(resource, 'delete');
    },

    async deleteMany(resource: string): Promise<any> {
        return unsupported(resource, 'deleteMany');
    },

    async sendNotification(request, idempotencyKey) {
        const response = await api.post<NotificationAccepted>(NOTIFICATIONS, request, { 'Idempotency-Key': idempotencyKey });
        return { ...response.body, replayed: response.headers.get('Idempotent-Replayed') === 'true' };
    },

    async sendBatch(items, idempotencyKey) {
        const response = await api.post<BatchResponse>(`${NOTIFICATIONS}/batch`, { items }, {
            'Idempotency-Key': idempotencyKey,
        });
        return response.body;
    },

    async cancelNotification(id) {
        return (await api.post<NotificationView>(`${NOTIFICATIONS}/${enc(id)}/cancel`)).body;
    },

    getTemplatesByKey(key) {
        return api.get<TemplateView[]>(`${TEMPLATES}/${enc(key)}`);
    },

    async addTemplateVersion(key, request) {
        return (await api.post<TemplateView>(`${TEMPLATES}/${enc(key)}/versions`, request)).body;
    },

    async activateTemplateVersion(key, variant, version) {
        return (await api.post<TemplateView>(`${TEMPLATES}/${enc(key)}/activate`, { ...variant, version })).body;
    },

    async previewTemplate(key, request) {
        return (await api.post<TemplatePreview>(`${TEMPLATES}/${enc(key)}/preview`, request)).body;
    },

    patchPreferences(recipientId, request) {
        return api.patch<PreferencesView>(`/api/v1/recipients/${enc(recipientId)}/preferences`, request);
    },

    getStatistics(query) {
        return api.get<StatisticsView>('/api/v1/statistics', {
            from: toInstant(query.from),
            to: toInstant(query.to),
            channel: query.channel,
            granularity: query.granularity,
        });
    },

    async getHealth() {
        return (await apiRequest<HealthComponent>('/actuator/health', { okStatuses: [503] })).body;
    },

    async getInfo() {
        return api.get<Record<string, unknown>>('/actuator/info');
    },

    async getMetric(name, tags = {}) {
        // Actuator expects repeated ?tag=key:value parameters.
        const search = Object.entries(tags)
            .map(([tag, value]) => `tag=${enc(`${tag}:${value}`)}`)
            .join('&');
        try {
            return await api.get<MetricResponse>(`/actuator/metrics/${enc(name)}${search ? `?${search}` : ''}`);
        } catch (error) {
            // 404: no such meter yet (nothing was sent through the channel).
            if ((error as { status?: number }).status === 404) return null;
            throw error;
        }
    },
};
