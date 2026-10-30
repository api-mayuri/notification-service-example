import type { Category, NotificationRequest, Priority } from '../api/types';
import { entriesToMap, parseVariableValue, type Entry } from '../components/KeyValueInput';
import { toInstant } from '../api/dataProvider';

/** Shape of the "send notification" form. */
export interface NotificationFormValues {
    idempotencyKey?: string;
    recipientId?: string;
    contacts?: Partial<Record<string, string>>;
    extraContacts?: Entry[];
    mode?: 'template' | 'content';
    templateKey?: string;
    templateLocale?: string;
    variables?: Entry[];
    subject?: string;
    text?: string;
    html?: string;
    channels?: string[];
    category?: Category;
    priority?: Priority;
    sendAt?: unknown;
    metadata?: Entry[];
}

const blank = (value: string | undefined | null) => !value || !value.trim();

/** Form values → NotificationRequest (the same JSON for REST and Kafka). */
export function buildNotificationRequest(values: NotificationFormValues): NotificationRequest {
    const contacts: Record<string, string> = {};
    for (const [type, value] of Object.entries(values.contacts ?? {})) {
        if (!blank(value)) contacts[type] = value!.trim();
    }
    Object.assign(contacts, entriesToMap<string>(values.extraContacts, value => String(value ?? '').trim()) ?? {});

    const request: NotificationRequest = {
        recipient: {
            id: (values.recipientId ?? '').trim(),
            ...(Object.keys(contacts).length ? { contacts } : {}),
        },
        channels: values.channels ?? [],
        category: values.category ?? 'TRANSACTIONAL',
    };
    if ((values.mode ?? 'template') === 'template') {
        request.template = {
            key: (values.templateKey ?? '').trim(),
            ...(blank(values.templateLocale) ? {} : { locale: values.templateLocale!.trim() }),
            ...(entriesToMap(values.variables, parseVariableValue) ? { variables: entriesToMap(values.variables, parseVariableValue) } : {}),
        };
    } else {
        request.content = {
            ...(blank(values.subject) ? {} : { subject: values.subject }),
            ...(blank(values.text) ? {} : { text: values.text }),
            ...(blank(values.html) ? {} : { html: values.html }),
        };
    }
    if (values.priority && values.priority !== 'NORMAL') request.priority = values.priority;
    const sendAt = toInstant(values.sendAt);
    if (sendAt) request.sendAt = sendAt;
    const metadata = entriesToMap<string>(values.metadata, value => String(value ?? ''));
    if (metadata) request.metadata = metadata;
    return request;
}

const shellQuote = (value: string) => `'${value.replace(/'/g, `'\\''`)}'`;

export function toCurl(request: NotificationRequest, idempotencyKey: string, baseUrl: string): string {
    const origin = baseUrl || window.location.origin;
    return [
        `curl -i -X POST ${origin}/api/v1/notifications \\`,
        `  -H "Authorization: Bearer $TOKEN" \\`,
        `  -H ${shellQuote(`Idempotency-Key: ${idempotencyKey}`)} \\`,
        `  -H 'Content-Type: application/json' \\`,
        `  -d ${shellQuote(JSON.stringify(request, null, 2))}`,
    ].join('\n');
}

/**
 * Same request as a CloudEvents command for notification.send.requests.v1 (binary mode),
 * ready for kafka-console-producer inside the compose Kafka container.
 */
export function toKafkaCommand(request: NotificationRequest, idempotencyKey: string, source: string): string {
    const payload = JSON.stringify({ ...request, idempotencyKey });
    const headers = [
        `ce_id:${idempotencyKey}`,
        `ce_source:${source}`,
        'ce_type:kz.company.notification.send.v1',
        'ce_specversion:1.0',
        'content-type:application/json',
    ].join(',');
    return [
        `docker compose exec -T kafka bash -c '`,
        `cat <<"EOF" | /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \\`,
        `  --topic notification.send.requests.v1 \\`,
        `  --property parse.key=true --property parse.headers=true \\`,
        `  --property headers.delimiter="|" --property key.separator=";"`,
        `${headers}|${request.recipient.id};${payload}`.replace(/'/g, `'\\''`),
        `EOF'`,
    ].join('\n');
}
