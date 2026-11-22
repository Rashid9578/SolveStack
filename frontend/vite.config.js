import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'API_PROXY_TARGET');
  const proxy = {
    '/api': env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
  };

  return { server: { proxy }, preview: { proxy } };
});
