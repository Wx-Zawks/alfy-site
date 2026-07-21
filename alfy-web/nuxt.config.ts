export default defineNuxtConfig({
  compatibilityDate: '2026-07-01',
  devtools: { enabled: true },
  components: [{ path: '~/components', pathPrefix: false }],
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080/api/v1'
    }
  },
  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      titleTemplate: '%s｜奥飞新材',
      meta: [
        {
          name: 'description',
          content: '奥飞新材新一代气凝胶及其复合产品技术'
        }
      ]
    }
  }
})
