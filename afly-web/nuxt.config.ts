export default defineNuxtConfig({
  compatibilityDate: '2026-07-01',
  devtools: { enabled: true },
  components: [{ path: '~/components', pathPrefix: false }],
  css: ['~/assets/css/main.css', '~/assets/css/mobile.css'],
  routeRules: {
    // 静态图片带长缓存：浏览器一周内不再重复下载（文件名不变时更新图片需清 CDN/浏览器缓存）。
    '/images/**': {
      headers: { 'cache-control': 'public, max-age=604800, immutable' }
    },
    // 构建产物带 hash，可永久缓存。
    '/_nuxt/**': {
      headers: { 'cache-control': 'public, max-age=31536000, immutable' }
    }
  },
  runtimeConfig: {
    // Production SSR runs inside the Docker network, where the API service is
    // available by its Compose service name. Keep this private: browsers must
    // continue to use the same-origin public route below.
    apiInternalBase: '',
    public: {
      apiBase: 'http://localhost:8080/api/v1',
      contentRefreshIntervalMs: 120000
    }
  },
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      titleTemplate: '%s｜奥飞新材',
      link: [{ rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }],
      meta: [
        {
          name: 'description',
          content: '奥飞新材新一代气凝胶及其复合产品技术'
        }
      ]
    }
  }
})
