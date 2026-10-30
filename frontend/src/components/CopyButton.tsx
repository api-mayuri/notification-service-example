import { IconButton, Tooltip } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useNotify, useTranslate } from 'react-admin';

export async function copyText(text: string): Promise<void> {
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return;
    }
    // Fallback for plain HTTP hosts where the Clipboard API is unavailable.
    const area = document.createElement('textarea');
    area.value = text;
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    area.remove();
}

export const CopyButton = ({ text, size = 'small' }: { text: string; size?: 'small' | 'medium' }) => {
    const notify = useNotify();
    const translate = useTranslate();
    return (
        <Tooltip title={translate('app.action.copy')}>
            <IconButton
                size={size}
                onClick={event => {
                    event.stopPropagation();
                    copyText(text).then(
                        () => notify('app.message.copied', { type: 'info' }),
                        () => notify('app.error.copy_failed', { type: 'warning' }),
                    );
                }}
            >
                <ContentCopyIcon fontSize="inherit" />
            </IconButton>
        </Tooltip>
    );
};
