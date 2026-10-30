import { Box } from '@mui/material';

/** Renders untrusted HTML (templates, previews) in a sandboxed iframe without scripts. */
export const HtmlPreview = ({ html, height = 320 }: { html: string; height?: number }) => (
    <Box
        component="iframe"
        title="html-preview"
        sandbox=""
        srcDoc={html}
        sx={{ width: '100%', height, border: 1, borderColor: 'divider', borderRadius: 1, bgcolor: '#fff' }}
    />
);
