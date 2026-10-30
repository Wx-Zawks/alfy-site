import type { ApiSeoGlobalSetting, ApiSeoRouteSetting } from '~/types/seo'
import { normalizeSiteOrigin } from '~/utils/seo-url'

const fallbackSettings: ApiSeoGlobalSetting = {
  baiduAnalyticsEnabled: false,
  canonicalOrigin: 'https://alfy.com.cn',
  clarityEnabled: false,
  defaultDescription: '湖南奥飞新材料有限公司官方网站。',
  defaultOgImageUrl: '/images/about-hero.webp',
  defaultTitle: '奥飞新材',
  ga4Enabled: false,
  googleExtendedPolicy: 'allow',
  gptbotPolicy: 'allow',
  oaiSearchbotPolicy: 'allow',
  organizationName: '湖南奥飞新材料有限公司',
  publicIndexEnabled: true,
  sameAs: [],
  titleTemplate: '%s｜奥飞新材'
}

function runtimeIndexEnabled(value: unknown) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return !['0', 'false', 'no', 'off'].includes(value.toLowerCase())
  return true
}

export async function useGlobalSeo() {
  const config = useRuntimeConfig()
  const [{ data: settingsData }, { data: routeData }] = await Promise.all([
    useApi<ApiSeoGlobalSetting>('public-seo-settings', '/public/seo/settings', { optional: true }),
    useApi<ApiSeoRouteSetting[]>('public-seo-routes', '/public/seo/routes', { optional: true })
  ])
  const settings = computed<ApiSeoGlobalSetting>(() => {
    const configured = settingsData.value
    return {
      ...fallbackSettings,
      ...configured,
      canonicalOrigin: normalizeSiteOrigin(
        configured?.canonicalOrigin || String(config.public.siteOrigin || fallbackSettings.canonicalOrigin)
      ),
      defaultOgImageUrl: configured?.defaultOgImageUrl || fallbackSettings.defaultOgImageUrl,
      publicIndexEnabled: runtimeIndexEnabled(config.public.siteIndexEnabled)
        && configured?.publicIndexEnabled !== false,
      sameAs: configured?.sameAs ?? []
    }
  })
  const routes = computed(() => routeData.value ?? [])
  const routeByPath = (path: string) => routes.value.find(item => item.routePath === path)
  return { routeByPath, routes, settings }
}
