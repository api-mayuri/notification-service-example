import { describe, expect, it } from 'vitest';
import { detectFormat, renderPreviewDocument, substituteVariables } from './render';

describe('substituteVariables', () => {
    const vars = { name: 'Анна <A>', user: { city: 'Almaty' }, url: 'https://x.kz/?a=1&b=2' };

    it('handles inline Thymeleaf expressions', () => {
        expect(substituteVariables('Привет, [(${name})]! [[${name}]] [(${user.city})]', vars)).toBe('Привет, Анна <A>! Анна &lt;A&gt; Almaty');
    });

    it('replaces th:text content and th:href attributes', () => {
        expect(substituteVariables('<p class="x" th:text="${name}">клиент</p><a th:href="${url}">go</a>', vars)).toBe(
            '<p class="x">Анна &lt;A&gt;</p><a href="https://x.kz/?a=1&amp;b=2">go</a>',
        );
        expect(substituteVariables('<b th:utext="${name}">x</b>', vars)).toBe('<b>Анна <A></b>');
    });

    it('keeps unknown variables visible', () => {
        expect(substituteVariables('Заказ [(${orderId})] <b th:text="${orderId}">ORD</b>', {})).toBe('Заказ ${orderId} <b>ORD</b>');
    });
});

describe('detectFormat', () => {
    it('detects html, markdown and text', () => {
        expect(detectFormat('<!DOCTYPE html><html><body>x</body></html>')).toBe('html');
        expect(detectFormat('<style>.a{}</style><div class="a">x</div>')).toBe('html');
        expect(detectFormat('# Заказ\n\n**ORD-42** отправлен')).toBe('markdown');
        expect(detectFormat('См. [трекинг](https://t.kz)')).toBe('markdown');
        expect(detectFormat('Код для сброса пароля: 1234. Никому его не сообщайте.')).toBe('text');
    });
});

describe('renderPreviewDocument', () => {
    it('renders markdown to html', () => {
        const doc = renderPreviewDocument('# Hi\n\n**bold** and [link](https://x.kz)', 'markdown');
        expect(doc).toContain('<h1>Hi</h1>');
        expect(doc).toContain('<strong>bold</strong>');
        expect(doc).toContain('<a href="https://x.kz">link</a>');
    });

    it('escapes plain text and keeps full html documents as is', () => {
        expect(renderPreviewDocument('a < b', 'text')).toContain('a &lt; b');
        const full = '<html><head><style>p{color:red}</style></head><body><p>x</p></body></html>';
        expect(renderPreviewDocument(full, 'html')).toBe(full);
        expect(renderPreviewDocument('<p>fragment</p>', 'html')).toContain('<body><p>fragment</p></body>');
    });
});
