import type { ApiEnvelope } from '~/composables/useApi'
import type { ApiSeoGlobalSetting, ApiSeoUrl } from '~/types/seo'
import { buildCanonical, normalizeSiteOrigin } from '~/utils/seo-url'

function runtimeIndexEnabled(value: unknown) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return !['0', 'false', 'no', 'off'].includes(value.toLowerCase())
  return true
}

function escapeXml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

function sitemapDate(value?: null | string) {
  return value?.match(/^\d{4}-\d{2}-\d{2}/)?.[0] || ''
}

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const baseURL = String(config.apiInternalBase || config.public.apiBase)
  let settings: ApiSeoGlobalSetting | null = null
  let urls: ApiSeoUrl[] = []
  try {
    const [settingsResponse, urlsResponse] = await Promise.all([
      $fetch<ApiEnvelope<ApiSeoGlobalSetting>>('/public/seo/settings', { baseURL }),
      $fetch<ApiEnvelope<ApiSeoUrl[]>>('/public/seo/urls', { baseURL })
    ])
    settings = settingsResponse.code === 0 ? settingsResponse.data : null
    urls = urlsResponse.code === 0 ? urlsResponse.data : []
  } catch {
    urls = [{ loc: '/', type: 'route' }]
  }
  const origin = normalizeSiteOrigin(settings?.canonicalOrigin || String(config.public.siteOrigin || ''))
  const indexEnabled = runtimeIndexEnabled(config.public.siteIndexEnabled)
    && settings?.publicIndexEnabled !== false
  if (!indexEnabled) urls = []
  const entries = [...new Map(urls
    .filter(item => item.loc.startsWith('/') && !item.loc.includes('?') && !item.loc.includes('#'))
    .map((item) => {
      const canonical = buildCanonical(origin, item.loc)
      return [canonical, { ...item, canonical }]
    })).values()]
    .map((item) => {
      const lastModified = sitemapDate(item.lastModified)
      const lastmod = lastModified ? `\n    <lastmod>${escapeXml(lastModified)}</lastmod>` : ''
      return `  <url>\n    <loc>${escapeXml(item.canonical)}</loc>${lastmod}\n  </url>`
    })
    .join('\n')
  setHeader(event, 'content-type', 'application/xml; charset=utf-8')
  setHeader(event, 'cache-control', 'public, max-age=600, s-maxage=600')
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries}\n</urlset>\n`
})
