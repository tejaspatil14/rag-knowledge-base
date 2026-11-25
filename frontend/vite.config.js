
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  const backend =
      env.BACKEND_URL || 'http://localhost:2000';

  return {
    plugins: [react()],

    server: {
      port: 5173,

      proxy: {
        '/api': {
          target: backend,
          changeOrigin: true,

          headers: env.RAG_API_KEY
              ? { 'X-API-Key': env.RAG_API_KEY }
              : {},
        },
      },
    },
  };
});
