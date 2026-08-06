import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import ElementPlus from 'unplugin-element-plus/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, fileURLToPath(new URL('.', import.meta.url)));
  return {
    base: env.VITE_BASE || '/',
    plugins: [vue(), vueJsx(), ElementPlus({ format: 'esm' })],
    resolve: {
      conditions: ['development'],
      alias: [
        {
          find: '@vben-core/design/bem',
          replacement: fileURLToPath(
            new URL(
              '../../packages/@core/base/design/src/scss-bem/bem.scss',
              import.meta.url,
            ),
          ),
        },
        {
          find: '@vben-core/design',
          replacement: fileURLToPath(
            new URL(
              '../../packages/@core/base/design/src/index.ts',
              import.meta.url,
            ),
          ),
        },
        {
          find: '#',
          replacement: fileURLToPath(new URL('src', import.meta.url)),
        },
      ],
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_PORT || 5777),
      proxy: {
        '/api': {
          changeOrigin: true,
          target: 'http://localhost:8080',
        },
      },
    },
  };
});
