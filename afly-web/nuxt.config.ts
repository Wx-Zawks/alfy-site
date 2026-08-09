export default defineNuxtConfig({
  compatibilityDate: '2026-07-01',
  devtools: { enabled: true },
  components: [{ path: '~/components', pathPrefix: false }],
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    // Production SSR runs inside the Docker network, where the API service is
    // available by its Compose service name. Keep this private: browsers must
    // continue to use the same-origin public route below.
    apiInternalBase: '',
    public: {
      apiBase: 'http://localhost:8080/api/v1',
      contentRefreshIntervalMs: 15000
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
