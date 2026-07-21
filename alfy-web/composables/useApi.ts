export function useApi<T>(url: string | (() => string), options: Record<string, unknown> = {}) {
  const config = useRuntimeConfig()

  return useFetch<T>(url, {
    baseURL: config.public.apiBase,
    ...options
  } as never)
}
