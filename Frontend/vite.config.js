import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

const proxyPaths = [
  '/auth',
  '/post',
  '/reply',
  '/user',
  '/maintenance',
  '/healthz',
  '/actuator',
  '/openapi.yaml',
  '/v3',
];

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_API_TARGET || 'http://localhost:9000';
  const proxy = Object.fromEntries(
    proxyPaths.map((path) => [
      path,
      {
        target,
        changeOrigin: true,
        secure: false,
      },
    ]),
  );

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: false,
      proxy,
    },
    preview: {
      port: 4173,
      strictPort: false,
      proxy,
    },
  };
});
