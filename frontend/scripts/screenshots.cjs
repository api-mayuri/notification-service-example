// Screenshots of every admin UI page in English → docs/screenshots (see docs/screenshots/README.md).
// Needs the compose stack with some data (send a few notifications first) and Playwright:
//   npm i --no-save playwright && npx playwright install chromium
//   BASE=http://localhost:3000 node scripts/screenshots.cjs ../docs/screenshots
const { chromium } = require('playwright');
const BASE = process.env.BASE || 'http://localhost:3000';
const OUT = process.argv[2];

(async () => {
    const auth = await (await fetch(`${BASE}/dev/token`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ clientId: 'admin-ui' }),
    })).json();
    const api = async path => (await fetch(`${BASE}${path}`, { headers: { Authorization: `Bearer ${auth.access_token}` } })).json();
    const list = (await api('/api/v1/notifications?size=100')).items;
    const emailDone = list.find(n => n.recipientId === 'user-42' && n.status === 'COMPLETED');
    const sms = list.find(n => n.channels.includes('SMS') && n.status === 'COMPLETED');

    const browser = await chromium.launch();
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'en-US', timezoneId: 'Asia/Almaty' });
    await context.addInitScript(() => {
        if (!localStorage.getItem('RaStore.locale')) localStorage.setItem('RaStore.locale', '"en"');
    });
    const page = await context.newPage();
    let index = 0;
    const shot = async (name, full = true) => {
        name = `${String(++index).padStart(2, '0')}-${name.replace(/^\d+-/, '')}`;
        // Hide-on-scroll app bar and the focus-only "Skip to content" button spoil full-page shots.
        if (full) await page.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
        await page.mouse.move(0, 0);
        await page.waitForTimeout(900);
        await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: full });
        console.log('saved', name);
    };
    const go = async (path, waitText) => {
        await page.goto(`${BASE}/#${path}`);
        if (waitText) await page.getByText(waitText, { exact: true }).first().waitFor({ timeout: 20000 });
    };

    await page.goto(BASE);
    await page.getByText('Dev token', { exact: true }).waitFor();
    await shot('01-login', false);
    await page.locator('input').first().fill('admin-ui');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.getByText('Notification service', { exact: true }).waitFor();
    await page.getByText('Total deliveries').waitFor();
    await shot('02-dashboard');

    await go('/notifications', 'Recipient');
    await page.getByText('user-42').first().waitFor();
    await shot('03-notifications-list');

    await go(`/notifications/${emailDone.id}/show`, 'Deliveries');
    await shot('04-notification-email');
    await go(`/notifications/${sms.id}/show`, 'Deliveries');
    await page.getByText('stub', { exact: true }).first().waitFor();
    await shot('05-notification-sms-stub');

    await go('/notifications/create', 'Recipient contacts');
    await page.locator('input[name="recipientId"]').fill('user-42');
    await page.getByRole('checkbox', { name: 'SMS' }).check();
    await page.locator('input[name="contacts.email"]').fill('anna@example.com');
    await page.locator('input[name="contacts.phone"]').fill('+77011234567');
    await page.getByText('order-shipped', { exact: true }).first().click();
    await page.locator('input[name="variables.0.value"]').fill('ORD-42');
    await page.getByText('Request: JSON, curl, Kafka').click();
    await page.getByRole('tab', { name: 'Kafka (CloudEvents)' }).click();
    await shot('06-send-notification');

    await go('/notifications/batch', 'Send batch');
    await page.getByRole('button', { name: 'Send batch' }).click();
    await page.getByText('Accepted: 2', { exact: true }).waitFor();
    await shot('07-batch-send');

    await go('/templates', 'order-shipped');
    await shot('08-templates-list');
    await go('/templates/00000000-0000-7000-8000-000000000006/show', 'Versions');
    await shot('09-template-versions');
    await go('/templates/00000000-0000-7000-8000-000000000003/show', 'Versions');
    await page.getByRole('tab', { name: 'Preview' }).click();
    await page.getByLabel('resetUrl').fill('https://example.com/reset?token=42');
    await page.getByLabel('validMinutes').fill('30');
    await page.getByRole('button', { name: 'Preview' }).last().click();
    await page.getByText('v1 · ru').waitFor();
    await shot('10-template-preview');
    // Live preview: HTML + CSS email template.
    await go('/templates/create', 'Required variables');
    await page.locator('input[name="key"]').fill('order-delivered');
    await page.locator('input[name="subject"]').fill('Order [(${orderId})] delivered');
    await page.getByRole('button', { name: 'Insert <style> (CSS)' }).click();
    const body = page.locator('textarea[name="body"]');
    await body.press('ControlOrMeta+End');
    await body.pressSequentially(
        '<div class="container"><h2>Hi, <span th:text="${name}">customer</span>!</h2>' +
            '<p>Your order <b th:text="${orderId}">ORD</b> has been delivered.</p>' +
            '<a class="button" th:href="${trackingUrl}">Rate the delivery</a></div>',
        { delay: 0 },
    );
    await page.getByRole('button', { name: 'Detect variables' }).click();
    await page.getByLabel('name', { exact: true }).fill('Anna');
    await page.getByLabel('orderId', { exact: true }).fill('ORD-42');
    await shot('11-template-create-html-css');

    // Live preview: Markdown for a text channel, mobile width (reload: a fresh form).
    await page.goto(`${BASE}/#/templates/create`);
    await page.reload();
    await page.getByText('Required variables', { exact: true }).first().waitFor();
    await page.locator('input[name="key"]').fill('order-shipped-md');
    await page.getByRole('combobox').first().click();
    await page.getByRole('option', { name: 'TELEGRAM' }).click();
    await page.locator('input[name="subject"]').fill('Order [(${orderId})] shipped');
    await page.locator('textarea[name="body"]').fill(
        '## Order [(${orderId})] shipped\n\n**Courier:** [(${courier})]\n\n- Track: [link](https://example.com/t/42)\n- Questions? Reply to this message',
    );
    await page.getByLabel('orderId', { exact: true }).fill('ORD-42');
    await page.getByLabel('courier', { exact: true }).fill('Kazpost');
    await page.locator('button[value="mobile"]').click();
    await shot('12-template-create-markdown-mobile');

    // Live preview in the "new version" dialog (editing a template).
    await go('/templates/00000000-0000-7000-8000-000000000006/show', 'Versions');
    await page.getByRole('button', { name: 'New version based on this' }).first().click();
    await page.getByRole('dialog').getByLabel('orderId', { exact: true }).fill('ORD-77');
    await page.setViewportSize({ width: 1440, height: 1000 });
    await shot('13-template-new-version-preview', false);
    await page.keyboard.press('Escape');
    await page.setViewportSize({ width: 1440, height: 900 });

    await go('/preferences?recipientId=user-42', 'Unsubscribed channels');
    await shot('12-preferences');

    await go('/statistics', 'Totals by channel');
    await shot('13-statistics');

    await go('/channels', 'EMAIL');
    await page.getByText('CLOSED').first().waitFor({ timeout: 20000 });
    await page.getByText(/stub: \d+/).first().waitFor({ timeout: 20000 });
    await shot('14-channels-and-providers');
    await page.getByRole('button', { name: 'Stub log' }).first().click();
    await page.getByText(/Stub SMS provider would send/).first().waitFor();
    await shot('15-stub-log', false);
    await page.getByRole('button', { name: 'Close' }).click();
    await page.getByRole('button', { name: 'Connect provider' }).nth(1).click();
    await page.getByRole('dialog').getByText('SMS via Twilio Messaging API.').click();
    await page.getByRole('button', { name: 'Next' }).click();
    await page.getByLabel(/account-sid/).fill('AC0123456789');
    await page.getByLabel(/^from/).fill('+15005550006');
    await shot('16-connect-provider-settings', false);
    await page.getByRole('button', { name: 'Next' }).click();
    await page.getByRole('tab', { name: 'Java' }).click();
    await shot('17-connect-provider-java', false);
    await page.getByRole('button', { name: 'Close' }).click();
    await page.getByRole('button', { name: 'Signature check' }).click();
    await page.getByRole('button', { name: 'Sign' }).click();
    await page.getByRole('button', { name: 'Verify' }).click();
    await page.getByRole('dialog').getByText(/Signature is valid/).waitFor();
    await shot('18-webhook-signature', false);
    await page.getByRole('button', { name: 'Close' }).click();

    await go('/system', 'Health');
    await page.getByText('readinessState').waitFor();
    await shot('19-system');

    await page.evaluate(() => localStorage.setItem('RaStore.theme', '"dark"'));
    await go('/', 'Notification service');
    await page.getByText('Total deliveries').waitFor();
    await shot('20-dashboard-dark');

    await browser.close();
})().catch(e => { console.error('FAILED', e.message.split('\n').slice(0, 4).join(' | ')); process.exit(1); });
