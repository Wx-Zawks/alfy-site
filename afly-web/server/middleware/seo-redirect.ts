import type { ApiEnvelope } from '~/composables/useApi'
import type { ApiRedirectResolve } from '~/types/seo'

const SKIP_PREFIXES = ['/_nuxt/', '/api/', '/images/', '/favicon', '/robots.txt', '/sitemap.xml']
const STATIC_FILE_PATTERN = /\.[a-z0-9]{2,8}$/i

export default defineEventHandler(async (event) => {
  const requestUrl = getRequestURL(event)
  if (SKIP_PREFIXES.some(prefix => requestUrl.pathname.startsWith(prefix))
    || STATIC_FILE_PATTERN.test(requestUrl.pathname)) return
  const config = useRuntimeConfig(event)
  const baseURL = String(config.apiInternalBase || config.public.apiBase)
  try {
    const response = await $fetch<ApiEnvelope<ApiRedirectResolve | null>>('/public/redirects/resolve', {
      baseURL,
      cache: 'no-store',
      query: {
        host: requestUrl.host,
        path: requestUrl.pathname,
        query: requestUrl.search.slice(1)
      }
    })
    if (response.code === 0 && response.data?.targetUrl) {
      return sendRedirect(event, response.data.targetUrl, response.data.statusCode)
    }
  } catch {
    // SEO 配置服务短暂不可用时继续渲染原请求，不能拖垮官网。
  }
})
