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
  const baseURL = String(config.public.apiBase || '/api/v1')

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
    if (/^https?:\/\//i.test(baseURL)) {
      return new URL(value, baseURL).toString()
    }
    return value
  }

  return { baseURL, request, resolveMediaUrl }
}

export async function useApi<T>(
  key: string,
  url: MaybeRefOrGetter<string>,
  options: ApiRequestOptions = {}
) {
  const { request } = useApiClient()
  return await useAsyncData<T | null>(key, () => request<T>(toValue(url), options), {
    default: () => null
  })
}
