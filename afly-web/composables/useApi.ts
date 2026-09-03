import type { MaybeRefOrGetter } from 'vue'

export interface ApiEnvelope<T> {
  code: number
  data: T
  message: string
}

export interface ApiRequestOptions {
  body?: unknown
  method?: 'DELETE' | 'GET' | 'POST' | 'PUT'
  optional?: boolean
  query?: Record<string, boolean | number | string | undefined>
}

function responseStatus(error: unknown) {
  const value = error as {
    response?: { status?: number }
    status?: number
    statusCode?: number
  }
  return value.statusCode ?? value.status ?? value.response?.status
}

export function useApiClient() {
  const config = useRuntimeConfig()
  const publicBaseURL = String(config.public.apiBase || '/api/v1')
  // On the server, a relative /api/v1 URL points back to the Nuxt process,
  // rather than the Spring Boot container. Use the private Docker-network URL
  // for SSR, while retaining the public same-origin URL in browsers.
  const baseURL = import.meta.server && config.apiInternalBase
    ? String(config.apiInternalBase)
    : publicBaseURL

  async function request<T>(url: string, options: ApiRequestOptions = {}) {
    const { optional, ...fetchOptions } = options
    try {
      const response = await $fetch<ApiEnvelope<T>>(url, {
        baseURL,
        // CMS 内容需要在发布后立即可重新获取，避免浏览器复用旧的 GET 响应。
        cache: !fetchOptions.method || fetchOptions.method === 'GET' ? 'no-store' : undefined,
        ...fetchOptions
      } as never)
      if (response.code !== 0) {
        throw createError({
          statusCode: response.code,
          statusMessage: response.message || '接口请求失败'
        })
      }
      return response.data
    } catch (error) {
      if (optional && responseStatus(error) === 404) return null
      throw error
    }
  }

  function resolveMediaUrl(value?: null | string, fallback = '') {
    if (!value) return fallback
    if (/^(?:data:|https?:\/\/)/i.test(value)) return value
    if (!value.startsWith('/')) return value
    if (/^https?:\/\//i.test(publicBaseURL)) {
      return new URL(value, publicBaseURL).toString()
    }
    return value
  }

  return { baseURL, request, resolveMediaUrl }
}

/**
 * 客户端导航短缓存：30 秒内往返同一页面时直接复用数据，
 * 不再重复回源后端。refreshNuxtData()（内容自动同步）会绕过该缓存，
 * 因此 CMS 发布的内容仍能按时生效。
 */
const CLIENT_CACHE_TTL_MS = 30_000
const clientDataCache = new Map<string, { data: unknown; expiresAt: number }>()

export async function useApi<T>(
  key: string,
  url: MaybeRefOrGetter<string>,
  options: ApiRequestOptions = {}
) {
  const { request } = useApiClient()
  return await useAsyncData<T | null>(
    key,
    async () => {
      const data = await request<T>(toValue(url), options)
      if (import.meta.client) {
        clientDataCache.set(key, { data, expiresAt: Date.now() + CLIENT_CACHE_TTL_MS })
      }
      return data
    },
    {
      default: () => null,
      getCachedData: (cacheKey, nuxtApp) => {
        if (import.meta.client) {
          const entry = clientDataCache.get(cacheKey)
          if (entry && entry.expiresAt > Date.now()) {
            return entry.data as T | null
          }
        }
        // 首次加载（SSR 水合）时复用 payload 中的数据，避免二次请求。
        if (nuxtApp.payload.data[cacheKey] !== undefined) {
          return nuxtApp.payload.data[cacheKey] as T | null
        }
        return undefined
      }
    }
  )
}
