import type { ApiEnvelope } from '~/composables/useApi'
import type { ApiSeoGlobalSetting } from '~/types/seo'
import { normalizeSiteOrigin } from '~/utils/seo-url'

function runtimeIndexEnabled(value: unknown) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return !['0', 'false', 'no', 'off'].includes(value.toLowerCase())
  return true
}

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const baseURL = String(config.apiInternalBase || config.public.apiBase)
  let settings: ApiSeoGlobalSetting | null = null
  try {
    const response = await $fetch<ApiEnvelope<ApiSeoGlobalSetting>>('/public/seo/settings', { baseURL })
    settings = response.code === 0 ? response.data : null
  } catch {
    settings = null
  }
  const origin = normalizeSiteOrigin(settings?.canonicalOrigin || String(config.public.siteOrigin || ''))
  const indexEnabled = runtimeIndexEnabled(config.public.siteIndexEnabled)
    && settings?.publicIndexEnabled !== false
  const lines = !indexEnabled
    ? ['User-agent: *', 'Disallow: /']
    : [
        'User-agent: *',
        'Allow: /',
        'Disallow: /api/',
        '',
        'User-agent: OAI-SearchBot',
        settings?.oaiSearchbotPolicy === 'disallow' ? 'Disallow: /' : 'Allow: /',
        '',
        'User-agent: GPTBot',
        settings?.gptbotPolicy === 'disallow' ? 'Disallow: /' : 'Allow: /',
        '',
        'User-agent: Google-Extended',
        settings?.googleExtendedPolicy === 'disallow' ? 'Disallow: /' : 'Allow: /',
        '',
        `Sitemap: ${origin}/sitemap.xml`
      ]
  setHeader(event, 'content-type', 'text/plain; charset=utf-8')
  setHeader(event, 'cache-control', 'public, max-age=300, s-maxage=300')
  return `${lines.join('\n')}\n`
})
