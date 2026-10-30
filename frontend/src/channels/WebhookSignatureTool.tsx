import { useState } from 'react';
import { useTranslate } from 'react-admin';
import { Alert, Box, Button, Stack, TextField } from '@mui/material';
import { CodeBlock } from '../components/CodeBlock';

/** HMAC-SHA256(secret, t + "." + body) as hex — same as WebhookSignature on the backend. */
export async function webhookSignature(secret: string, timestamp: number, body: string): Promise<string> {
    const encoder = new TextEncoder();
    const key = await crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
    const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(`${timestamp}.${body}`));
    return [...new Uint8Array(signature)].map(byte => byte.toString(16).padStart(2, '0')).join('');
}

export function parseSignatureHeader(header: string): { t?: number; v1?: string } {
    const parts = Object.fromEntries(
        header
            .split(',')
            .map(part => part.trim().split('=', 2))
            .filter(pair => pair.length === 2),
    );
    return { t: parts.t ? Number(parts.t) : undefined, v1: parts.v1 };
}

const SAMPLE_BODY = JSON.stringify({
    deliveryId: '0199…',
    notificationId: '0199…',
    recipientId: 'user-42',
    subject: 'Заказ ORD-42 отправлен',
    text: 'Ваш заказ ORD-42 передан в доставку.',
    html: null,
    timestamp: Math.floor(Date.now() / 1000),
});

/** Checks X-Notification-Signature of a received webhook, or signs a test body. */
export const WebhookSignatureTool = () => {
    const translate = useTranslate();
    const [secret, setSecret] = useState('local-webhook-secret');
    const [header, setHeader] = useState('');
    const [body, setBody] = useState(SAMPLE_BODY);
    const [result, setResult] = useState<{ ok: boolean; text: string } | null>(null);
    const available = typeof crypto !== 'undefined' && Boolean(crypto.subtle);

    const sign = async () => {
        const timestamp = Math.floor(Date.now() / 1000);
        const v1 = await webhookSignature(secret, timestamp, body);
        setHeader(`t=${timestamp},v1=${v1}`);
        setResult(null);
    };
    const verify = async () => {
        const { t, v1 } = parseSignatureHeader(header);
        if (!t || !v1) {
            setResult({ ok: false, text: translate('app.webhook.bad_header') });
            return;
        }
        const expected = await webhookSignature(secret, t, body);
        const age = Math.abs(Date.now() / 1000 - t);
        setResult(
            expected === v1
                ? { ok: age <= 300, text: translate(age <= 300 ? 'app.webhook.valid' : 'app.webhook.expired', { age: Math.round(age) }) }
                : { ok: false, text: translate('app.webhook.invalid', { expected }) },
        );
    };

    if (!available) return <Alert severity="warning">{translate('app.webhook.no_crypto')}</Alert>;
    return (
        <Box>
            <Stack spacing={2}>
                <TextField size="small" label="WEBHOOK_SIGNING_SECRET" value={secret} onChange={e => setSecret(e.target.value)} />
                <TextField size="small" label="X-Notification-Signature" value={header} onChange={e => setHeader(e.target.value)} placeholder="t=1760380000,v1=…" />
                <TextField
                    label={translate('app.webhook.body')}
                    value={body}
                    onChange={e => setBody(e.target.value)}
                    multiline
                    minRows={4}
                    slotProps={{ input: { sx: { fontFamily: 'monospace', fontSize: 12 } } }}
                />
                <Stack direction="row" spacing={1}>
                    <Button variant="outlined" onClick={sign}>
                        {translate('app.webhook.sign')}
                    </Button>
                    <Button variant="contained" onClick={verify}>
                        {translate('app.webhook.verify')}
                    </Button>
                </Stack>
                {result && <Alert severity={result.ok ? 'success' : 'error'}>{result.text}</Alert>}
                <CodeBlock code={`HMAC-SHA256(secret, t + "." + body) → hex\nX-Notification-Signature: t=<unix seconds>,v1=<hex>`} />
            </Stack>
        </Box>
    );
};
