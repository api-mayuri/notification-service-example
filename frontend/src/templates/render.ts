import { marked } from 'marked';

/**
 * Browser-side template preview while editing (nothing is saved or sent). Approximates the
 * Thymeleaf syntax the service uses: [(${x})] raw, [[${x}]] escaped, th:text / th:utext /
 * th:href / th:src with ${x} or ${a.b}. The exact render is the server preview of a saved version.
 */
export type PreviewFormat = 'html' | 'markdown' | 'text';

export const escapeHtml = (value: string) =>
    value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');

function lookup(variables: Record<string, unknown>, path: string): unknown {
    return path.split('.').reduce<unknown>((value, key) => (value && typeof value === 'object' ? (value as Record<string, unknown>)[key] : undefined), variables);
}

const asText = (value: unknown) => (typeof value === 'string' ? value : JSON.stringify(value));

/** Replaces Thymeleaf expressions; unknown variables stay visible as ${name}. */
export function substituteVariables(source: string, variables: Record<string, unknown>): string {
    const value = (path: string) => {
        const found = lookup(variables, path.trim());
        return found === undefined || found === '' ? undefined : asText(found);
    };
    return (
        source
            // <tag th:text="${x}">default</tag> → <tag>value</tag> (th:utext without escaping)
            .replace(/<([a-zA-Z][\w-]*)([^>]*?)\s+th:(u?)text="\$\{([^}]+)\}"([^>]*)>([\s\S]*?)<\/\1>/g, (_, tag, before, raw, path, after, inner) => {
                const v = value(path);
                const content = v === undefined ? inner : raw ? v : escapeHtml(v);
                return `<${tag}${before}${after}>${content}</${tag}>`;
            })
            // th:href / th:src / th:alt / th:title="${x}" → plain attribute
            .replace(/\sth:(href|src|alt|title)="\$\{([^}]+)\}"/g, (match, attr, path) => {
                const v = value(path);
                return v === undefined ? match : ` ${attr}="${escapeHtml(v)}"`;
            })
            .replace(/\[\(\$\{([^}]+)\}\)\]/g, (_, path) => value(path) ?? `\${${path.trim()}}`)
            .replace(/\[\[\$\{([^}]+)\}\]\]/g, (_, path) => {
                const v = value(path);
                return v === undefined ? `\${${path.trim()}}` : escapeHtml(v);
            })
    );
}

/** Guess how a body should be shown: HTML tags → html, Markdown syntax → markdown, otherwise text. */
export function detectFormat(body: string): PreviewFormat {
    if (/<(!doctype|html|body|div|p|table|span|a|b|strong|em|h[1-6]|br|img|ul|ol|li|style)\b[^>]*>/i.test(body)) return 'html';
    if (/^(#{1,6}\s|[-*+]\s|\d+\.\s|>\s|```)|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\)/m.test(body)) return 'markdown';
    return 'text';
}

const BASE_CSS = `body{margin:0;padding:16px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Arial,sans-serif;font-size:15px;line-height:1.5;color:#1f2328;background:#fff}
img{max-width:100%}pre,code{font-family:ui-monospace,Menlo,monospace;background:#f3f4f6;border-radius:4px}pre{padding:12px;overflow:auto}code{padding:1px 4px}
blockquote{margin:0;padding-left:12px;border-left:4px solid #d0d7de;color:#57606a}table{border-collapse:collapse}td,th{border:1px solid #d0d7de;padding:4px 8px}`;

/** Full HTML document for a sandboxed iframe (srcDoc). */
export function renderPreviewDocument(body: string, format: PreviewFormat): string {
    if (format === 'html') {
        // Full documents are shown as is; fragments get the base typography.
        return /<html[\s>]/i.test(body) ? body : `<!doctype html><html><head><meta charset="utf-8"><style>${BASE_CSS}</style></head><body>${body}</body></html>`;
    }
    const content =
        format === 'markdown'
            ? (marked.parse(body, { async: false, gfm: true, breaks: true }) as string)
            : `<div style="white-space:pre-wrap">${escapeHtml(body)}</div>`;
    return `<!doctype html><html><head><meta charset="utf-8"><style>${BASE_CSS}</style></head><body>${content}</body></html>`;
}

/** Skeleton of a <style> block for HTML templates (email clients honour head/inline styles differently). */
export const STYLE_SNIPPET = `<style>
  .container { max-width: 560px; margin: 0 auto; font-family: Arial, sans-serif; }
  .button { display: inline-block; padding: 10px 18px; background: #1976d2; color: #fff; border-radius: 6px; text-decoration: none; }
</style>
`;
