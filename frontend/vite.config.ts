/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server proxies backend paths, so the browser sees one origin (no CORS on the backend).
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const backend = env.DEV_BACKEND_URL || 'http://localhost:8080';
    const proxied = ['/api', '/dev', '/actuator', '/v3/api-docs', '/swagger-ui'];
    return {
        plugins: [react()],
        server: {
            port: Number(env.DEV_PORT || 5173),
            proxy: Object.fromEntries(proxied.map(path => [path, { target: backend, changeOrigin: true }])),
        },
        build: {
            outDir: 'dist',
            chunkSizeWarningLimit: 1500,
            rollupOptions: {
                output: {
                    manualChunks: (id: string) =>
                        id.includes('node_modules') ? (/recharts|d3-|victory/.test(id) ? 'charts' : 'vendor') : undefined,
                },
            },
        },
        test: {
            environment: 'jsdom',
            include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
        },
    };
});
