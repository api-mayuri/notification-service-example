import { Box } from '@mui/material';
import { CopyButton } from './CopyButton';

export const CodeBlock = ({ code, maxHeight = 400 }: { code: string; maxHeight?: number }) => (
    <Box sx={{ position: 'relative' }}>
        <Box
            component="pre"
            sx={{
                m: 0,
                p: 1.5,
                pr: 5,
                borderRadius: 1,
                bgcolor: 'action.hover',
                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                fontSize: 12.5,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                overflow: 'auto',
                maxHeight,
            }}
        >
            {code}
        </Box>
        <Box sx={{ position: 'absolute', top: 4, right: 4 }}>
            <CopyButton text={code} />
        </Box>
    </Box>
);
